package dio.budgeting.infrastructure.ai;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.PrebuiltVoiceConfig;
import com.google.genai.types.SpeechConfig;
import com.google.genai.types.VoiceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@Service
public class AudioSpeechService {
    private final Client genAiClient;
    private final String voiceName;
    private final String ttsModel;

    public AudioSpeechService(Client genAiClient,
                              @Value("${app.ai.tts.voice}") String voiceName,
                              @Value("${app.ai.tts.model}") String ttsModel) {
        this.genAiClient = genAiClient;
        this.voiceName = voiceName;
        this.ttsModel = ttsModel;
    }

    public byte[] synthesize(String text) {
        var speechConfig = SpeechConfig.builder()
                .voiceConfig(VoiceConfig.builder()
                        .prebuiltVoiceConfig(PrebuiltVoiceConfig.builder()
                                .voiceName(voiceName)
                                .build())
                        .build())
                .build();

        var config = GenerateContentConfig.builder()
                .responseModalities("AUDIO")
                .speechConfig(speechConfig)
                .build();

        var response = genAiClient.models.generateContent(ttsModel, text, config);

        byte[] rawPcm = response.candidates().get().get(0)
                .content().get()
                .parts().get().get(0)
                .inlineData().get()
                .data().get();

        return wrapPcmAsWav(rawPcm, 24000, 1, 16);
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
        buffer.putInt(16);
        buffer.putShort((short) 1);
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
