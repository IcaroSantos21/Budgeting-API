package dio.budgeting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "GOOGLE_GENAI_API_KEY", matches = ".+")
public class GenAIChatClientIT {

    @Autowired
    GoogleGenAiChatModel googleGenAiChatModel;

    @Test
    void should_executeSum_when_prompted() {
        var chatClient = ChatClient.builder(googleGenAiChatModel)
                .defaultSystem("Você é um matemático")
                .build();

        var response = chatClient
                .prompt("Some 10 mais 20, depois subtraia 30 do resultado anterior. Exiba apenas o resultado final sem explicações")
                .call()
                .content();

        assertThat(response).contains("0");
        System.out.println(response);
    }
}
