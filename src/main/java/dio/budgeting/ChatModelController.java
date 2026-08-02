package dio.budgeting;

import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ChatModelController {

    private final GoogleGenAiChatModel googleGenAiChatModel;

    public ChatModelController(GoogleGenAiChatModel googleGenAiChatModel) {
        this.googleGenAiChatModel = googleGenAiChatModel;
    }

    @GetMapping("/chat-model")
    String chat(String prompt) {
        return this.googleGenAiChatModel.call(prompt);
    }
}
