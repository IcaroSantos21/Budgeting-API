package dio.budgeting;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.PrebuiltVoiceConfig;
import com.google.genai.types.SpeechConfig;
import com.google.genai.types.VoiceConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MimeTypeUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "GOOGLE_GENAI_API_KEY", matches = ".+")
public class GenAiSpeechModelIT {

    @Autowired
    Client genAiClient;

    @Test
    void should_produceAudio_when_textIsProvided() throws IOException {
        var speechConfig = SpeechConfig.builder()
                .voiceConfig(VoiceConfig.builder()
                        .prebuiltVoiceConfig(PrebuiltVoiceConfig.builder()
                                .voiceName("Kore")
                                .build())
                        .build())
                .build();

        var config = GenerateContentConfig.builder()
                .responseModalities("AUDIO")
                .speechConfig(speechConfig)
                .build();

        var response = genAiClient.models.generateContent(
                "gemini-3.1-flash-tts-preview",
                "O valor total do serviço ficou em 80 reais. Posso confirmar o pagamento?",
                config
        );

        byte[] rawPcm = response.candidates().get().get(0)
                .content().get()
                .parts().get().get(0)
                .inlineData().get()
                .data().get();

        byte[] wavData = wrapPcmAsWav(rawPcm, 24000, 1, 16);

        assertThat(wavData).hasSizeGreaterThan(1024);

        var tempFile = Files.createTempFile("AUDIO_", ".ogg");
        Files.write(tempFile, wavData);
        System.out.println(tempFile.toAbsolutePath());
    }

    private byte[] wrapPcmAsWav(byte[] pcmData, int sampleRate, int channels, int bitsPerSample) {
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;
        int dataSize = pcmData.length;
        int chunkSize = 36 + dataSize;

        ByteBuffer buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put("RIFF".getBytes());
        buffer.putInt(chunkSize);
        buffer.put("WAVE".getBytes());
        buffer.put("fmt ".getBytes());
        buffer.putInt(16); // Subchunk1Size (PCM)
        buffer.putShort((short) 1); // AudioFormat = PCM
        buffer.putShort((short) channels);
        buffer.putInt(sampleRate);
        buffer.putInt(byteRate);
        buffer.putShort((short) blockAlign);
        buffer.putShort((short) bitsPerSample);
        buffer.put("data".getBytes());
        buffer.putInt(dataSize);
        buffer.put(pcmData);

        return buffer.array();
    }

}
