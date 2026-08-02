package dio.budgeting;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.PrebuiltVoiceConfig;
import com.google.genai.types.SpeechConfig;
import com.google.genai.types.VoiceConfig;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@RestController
@RequestMapping("/api")
public class TextToSpeechController {

    private final Client genAiClient;

    public TextToSpeechController(Client genAiClient) {
        this.genAiClient = genAiClient;
    }

    @PostMapping(value = "/synthesize", produces = "audio/ogg")
    public ResponseEntity<ByteArrayResource> synthesize(@RequestBody SynthesisRequest request) {
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
                request.text(),
                config
        );

        byte[] rawPcm = response.candidates().get().get(0)
                .content().get()
                .parts().get().get(0)
                .inlineData().get()
                .data().get();

        byte[] audio = wrapPcmAsWav(rawPcm, 24000, 1, 16);
        var resource = new ByteArrayResource(audio);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("audio.ogg")
                                .build()
                                .toString())
                .body(resource);
    }

    record SynthesisRequest(String text) {
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
