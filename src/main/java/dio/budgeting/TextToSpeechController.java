package dio.budgeting;

import dio.budgeting.infrastructure.ai.AudioSpeechService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TextToSpeechController {

    private final AudioSpeechService audioSpeechService;

    public TextToSpeechController(AudioSpeechService audioSpeechService) {
        this.audioSpeechService = audioSpeechService;
    }

    @PostMapping(value = "/synthesize", produces = "audio/ogg")
    public ResponseEntity<ByteArrayResource> synthesize(@RequestBody SynthesisRequest request) {
        var resource = new ByteArrayResource(audioSpeechService.synthesize(request.text()));

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
}