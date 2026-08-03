package dio.budgeting;

import dio.budgeting.infrastructure.ai.AudioTranscriptionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class TranscriptionController {

    private final AudioTranscriptionService audioTranscriptionService;

    public TranscriptionController(AudioTranscriptionService audioTranscriptionService) {
        this.audioTranscriptionService = audioTranscriptionService;
    }

    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    String transcribe(@RequestParam("file") MultipartFile file) throws IOException {
        return audioTranscriptionService.transcribe(file);
    }
}