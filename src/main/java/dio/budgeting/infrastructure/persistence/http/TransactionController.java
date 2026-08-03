package dio.budgeting.infrastructure.persistence.http;

import dio.budgeting.application.SumTransactionsAllUseCase;
import dio.budgeting.application.SumTransactionsByCategoryUseCase;
import dio.budgeting.infrastructure.persistence.http.response.SumResponse;
import org.springframework.http.HttpHeaders;
import com.google.genai.Client;
import dio.budgeting.application.ListTransactionsByCategoryUseCase;
import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.domain.Category;
import dio.budgeting.infrastructure.ai.AudioSpeechService;
import dio.budgeting.infrastructure.ai.AudioTranscriptionService;
import dio.budgeting.infrastructure.persistence.http.request.TransactionRequest;
import dio.budgeting.infrastructure.persistence.http.response.TransactionResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

@RestController
@RequestMapping("transactions")
public class TransactionController {

    private final ChatClient chatClient;
    private final AudioTranscriptionService audioTranscriptionService;
    private final AudioSpeechService audioSpeechService;

    private final PersistTransactionUseCase persistTransactionUseCase;
    private final ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase;
    private final SumTransactionsByCategoryUseCase sumTransactionsByCategoryUseCase;
    private final SumTransactionsAllUseCase sumTransactionsAllUseCase;

    public TransactionController(PersistTransactionUseCase persistTransactionUseCase,
                                 ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase,
                                 AudioSpeechService audioSpeechService,
                                 AudioTranscriptionService audioTranscriptionService,
                                 @Value("classpath:prompts/system-message.st") Resource systemPrompt,
                                 ChatClient.Builder chatClientBuilder,
                                 Client genAiClient, SumTransactionsByCategoryUseCase sumTransactionsByCategoryUseCase, SumTransactionsAllUseCase sumTransactionsAllUseCase) throws IOException {
        this.persistTransactionUseCase = persistTransactionUseCase;
        this.listTransactionsByCategoryUseCase = listTransactionsByCategoryUseCase;
        this.audioSpeechService = audioSpeechService;
        this.audioTranscriptionService = audioTranscriptionService;
        this.sumTransactionsByCategoryUseCase = sumTransactionsByCategoryUseCase;
        this.sumTransactionsAllUseCase = sumTransactionsAllUseCase;
        this.chatClient = chatClientBuilder
                .defaultSystem(systemPrompt.getContentAsString(Charset.defaultCharset()))
                .defaultTools(persistTransactionUseCase, listTransactionsByCategoryUseCase)
                .build();
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
        var userMessage = audioTranscriptionService.transcribe(file);
        var result = chatClient.prompt().user(userMessage).call().content();
        var resource = new ByteArrayResource(audioSpeechService.synthesize(result));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("audio.ogg")
                                .build()
                                .toString())
                .body(resource);
    }

    @GetMapping("/total")
    @ResponseStatus(HttpStatus.OK)
    public SumResponse sumTotal() {
        return SumResponse.from(sumTransactionsAllUseCase.execute());
    }

    @GetMapping("/{category}/total")
    @ResponseStatus(HttpStatus.OK)
    public SumResponse sumByCategory(@PathVariable Category category) {
        return SumResponse.from(sumTransactionsByCategoryUseCase.execute(category));
    }
}
