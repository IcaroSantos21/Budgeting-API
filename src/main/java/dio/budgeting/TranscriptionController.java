package dio.budgeting;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api")
public class TranscriptionController {

    private final ChatModel chatModel;

    public TranscriptionController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

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

    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    String transcribe(@RequestParam("file")MultipartFile file) throws IOException {

        var mimeType = MimeType.valueOf(Objects.requireNonNull(file.getContentType()));

        var userMessage = UserMessage.builder()
                .text(TRANSCRIPTION_PROMPT)
                .media(List.of(new Media(mimeType, file.getResource())))
                .build();

        var response = chatModel.call(new Prompt(List.of(userMessage)));

        return Objects.requireNonNull(response.getResult()).getOutput().getText();
    }
}
