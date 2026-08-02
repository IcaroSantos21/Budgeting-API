package dio.budgeting.infrastructure.persistence.http;

import com.google.common.net.HttpHeaders;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.PrebuiltVoiceConfig;
import com.google.genai.types.SpeechConfig;
import com.google.genai.types.VoiceConfig;
import dio.budgeting.application.ListTransactionsByCategoryUseCase;
import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.domain.Category;
import dio.budgeting.infrastructure.persistence.http.request.TransactionRequest;
import dio.budgeting.infrastructure.persistence.http.response.TransactionResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("transactions")
public class TransactionController {

    private final ChatModel chatModel;
    private final ChatClient chatClient;
    private final Client genAiClient;

    private final PersistTransactionUseCase persistTransactionUseCase;
    private final ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase;

    private static final String TRANSCRIPTION_PROMPT = """
            Áudio em português brasileiro.
            Áudio contém descrição de gastos financeiros.
            As frases geralmente contêm:
            - um valor em reais (número + "reais");
            - uma ação (gastei, paguei, comprei);
            - um local ou estabelecimento (mercado, farmácia, restaurante, loja, etc.).
            Sempre que o valor em reais for falado por extenso, converta para número em dígitos.
            Transcreva o áudio a seguir, retornando apenas o texto transcrito.
            """;

    public TransactionController(PersistTransactionUseCase persistTransactionUseCase,
                                 ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase,
                                 ChatModel chatModel,
                                 @Value("classpath:prompts/system-message.st") Resource systemPrompt,
                                 ChatClient.Builder chatClientBuilder,
                                 Client genAiClient) throws IOException {
        this.persistTransactionUseCase = persistTransactionUseCase;
        this.listTransactionsByCategoryUseCase = listTransactionsByCategoryUseCase;
        this.chatModel = chatModel;
        this.chatClient = chatClientBuilder
                .defaultSystem(systemPrompt.getContentAsString(Charset.defaultCharset()))
                .defaultTools(persistTransactionUseCase, listTransactionsByCategoryUseCase)
                .build();
        this.genAiClient = genAiClient;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(@RequestBody TransactionRequest request) {
        var transaction = persistTransactionUseCase.execute(request.toInput());
        return TransactionResponse.from(transaction);
    }

    @GetMapping("/{category}")
    @ResponseStatus(HttpStatus.OK)
    public List<TransactionResponse> readTransactions(@PathVariable Category category) {
        return listTransactionsByCategoryUseCase.execute(category)
                .stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @PostMapping(value = "/ai", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "audio/ogg")
    public ResponseEntity<Resource> transcribe(@RequestParam("file") MultipartFile file) throws IOException {
        var mimeType = MimeType.valueOf(Objects.requireNonNull(file.getContentType()));
        var audioMessage = UserMessage.builder()
                .text(TRANSCRIPTION_PROMPT)
                .media(List.of(new Media(mimeType, file.getResource())))
                .build();
        var transcriptionResponse = chatModel.call(new Prompt(List.of(audioMessage)));
        var userMessage = transcriptionResponse.getResult().getOutput().getText();

        assert userMessage != null;
        var result = chatClient.prompt().user(userMessage).call().content();

        var speechConfig = SpeechConfig.builder()
                .voiceConfig(VoiceConfig.builder()
                        .prebuiltVoiceConfig(PrebuiltVoiceConfig.builder()
                                .voiceName("Kore")
                                .build())
                        .build())
                .build();

        var ttsConfig = GenerateContentConfig.builder()
                .responseModalities("AUDIO")
                .speechConfig(speechConfig)
                .build();

        var ttsResponse = genAiClient.models.generateContent(
                "gemini-3.1-flash-tts-preview",
                result,
                ttsConfig
        );

        byte[] rawPcm = ttsResponse.candidates().get().get(0)
                .content().get()
                .parts().get().get(0)
                .inlineData().get()
                .data().get();
        byte[] audio = wrapPcmAsOgg(rawPcm, 22050, 1, 16);

        var resource = new ByteArrayResource(audio);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("audio.ogg")
                                .build()
                                .toString())
                .body(resource);
    }

    private byte[] wrapPcmAsOgg(byte[] pcmData, int sampleRate, int channels, int bitsPerSample) {
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
