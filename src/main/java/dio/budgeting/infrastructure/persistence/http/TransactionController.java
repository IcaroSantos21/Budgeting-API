package dio.budgeting.infrastructure.persistence.http;

import dio.budgeting.application.SumAllTransactionsUseCase;
import dio.budgeting.application.SumTransactionsByCategoryUseCase;
import dio.budgeting.infrastructure.persistence.http.response.SumResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final SumAllTransactionsUseCase sumTransactionsAllUseCase;

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    public TransactionController(PersistTransactionUseCase persistTransactionUseCase,
                                 ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase,
                                 AudioSpeechService audioSpeechService,
                                 AudioTranscriptionService audioTranscriptionService,
                                 @Value("classpath:prompts/system-message.st") Resource systemPrompt,
                                 ChatClient.Builder chatClientBuilder,
                                 Client genAiClient,
                                 SumTransactionsByCategoryUseCase sumTransactionsByCategoryUseCase,
                                 SumAllTransactionsUseCase sumAllTransactionsUseCase) throws IOException
    {

        this.persistTransactionUseCase = persistTransactionUseCase;
        this.listTransactionsByCategoryUseCase = listTransactionsByCategoryUseCase;
        this.audioSpeechService = audioSpeechService;
        this.audioTranscriptionService = audioTranscriptionService;
        this.sumTransactionsByCategoryUseCase = sumTransactionsByCategoryUseCase;
        this.sumTransactionsAllUseCase = sumAllTransactionsUseCase;
        this.chatClient = chatClientBuilder
                .defaultSystem(systemPrompt.getContentAsString(Charset.defaultCharset()))
                .defaultTools(
                        persistTransactionUseCase,
                        listTransactionsByCategoryUseCase,
                        sumAllTransactionsUseCase,
                        sumTransactionsByCategoryUseCase)
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
        log.info("1 - Recebi o arquivo");

        log.info("1.1 - Antes da Transcrição");
        var userMessage = audioTranscriptionService.transcribe(file);
        log.info("2 - Transcrito para {}", userMessage);

        var result = chatClient.prompt().user(userMessage).call().content();
        log.info("3 - Resultado do chat: {}", result);

        var resource = new ByteArrayResource(audioSpeechService.synthesize(result));
        log.info("4 - Audio gerado");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("response.ogg")
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
