package dio.budgeting.infrastructure.ai;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

@Service
public class AudioTranscriptionService {

    private final ChatModel chatModel;
    private final String transcriptionPrompt;

    public AudioTranscriptionService(ChatModel chatModel,
                                     @Value("classpath:prompts/transcription-prompt.st") Resource transcriptionPromptResource)
            throws IOException {
        this.chatModel = chatModel;
        this.transcriptionPrompt = transcriptionPromptResource.getContentAsString(Charset.defaultCharset());
    }

    public String transcribe(MultipartFile file) {
        var mimeType = MimeType.valueOf(Objects.requireNonNull(file.getContentType()));

        var userMessage = UserMessage.builder()
                .text(transcriptionPrompt)
                .media(List.of(new Media(mimeType, file.getResource())))
                .build();

        var response = chatModel.call(new Prompt(List.of(userMessage)));

        return Objects.requireNonNull(response.getResult()).getOutput().getText();
    }
}
