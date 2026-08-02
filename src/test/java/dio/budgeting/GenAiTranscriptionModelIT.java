package dio.budgeting;

import com.google.genai.Client;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "GOOGLE_GENAI_API_KEY", matches = ".+")
public class GenAiTranscriptionModelIT {

    @Autowired
    Client genAiClient;

    private static final String TRANSCRIPTION_PROMPT = """
            Áudio em português brasileiro.
            Áudio contém descrição de gastos financeiros.
            As frases geralmente contém:
            - um valor em reais (número + "reais");
            - uma ação (gastei, paguei, comprei)
            - um local, ou estabelecimento (mercado, farmácia, restaurante, loja, etc.)
            
            IMPORTANTE: sempre que o valor em reais for falado por extenso,
            converta para número em dígitos no texto transcrito.
            Exemplo: se o áudio disser "noventa reais", trasncreva como "90 reais".
            Exemplo: se o áudio disser "cento e vinte reais" transcreva como "120 reais"
            Transcreva o áudio a seguir, retornando apenas o texto transcrito.
            """;

    @ParameterizedTest
    @CsvSource({
            "recording_1.ogg, 90 reais",
            "recording_2.ogg, 50 reais",
            "recording_3.ogg, 200 reais",
            "recording_4.ogg, 60 reais"
    })
    void should_containExpectedKeywords_when_audioFilesAreProcessed(String fileName, String expectedKeywords) {
        var recording = new ClassPathResource("audio/" + fileName);

        var options = GoogleGenAiChatOptions.builder()
                .model("gemini-3.5-flash")
                .temperature(0.8)
                .responseMimeType("text/plain")
                .build();

        var chatModel = GoogleGenAiChatModel.builder()
                .genAiClient(genAiClient)
                .defaultOptions(options)
                .build();

        var userMessage = UserMessage.builder()
                .text(TRANSCRIPTION_PROMPT)
                .media(List.of(new Media(MimeTypeUtils.parseMimeType("audio/ogg"), recording)))
                .build();

        ChatResponse chatResponse = chatModel.call(new Prompt(List.of(userMessage), options));
        var response = chatResponse.getResult().getOutput().getText();

        assertThat(response).contains(expectedKeywords);
        System.out.println(response);

    }


}
