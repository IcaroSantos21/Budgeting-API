package dio.budgeting.infrastructure.persistence.http.exception;

import com.google.genai.Client;
import dio.budgeting.application.ListTransactionsByCategoryUseCase;
import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.application.SumAllTransactionsUseCase;
import dio.budgeting.application.SumTransactionsByCategoryUseCase;
import dio.budgeting.infrastructure.ai.AudioSpeechService;
import dio.budgeting.infrastructure.ai.AudioTranscriptionService;
import dio.budgeting.infrastructure.persistence.http.TransactionController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerExceptionHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PersistTransactionUseCase persistTransactionUseCase;

    @MockitoBean
    private ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase;

    @MockitoBean
    private SumAllTransactionsUseCase sumAllTransactionsUseCase;

    @MockitoBean
    private SumTransactionsByCategoryUseCase sumTransactionsByCategoryUseCase;

    @MockitoBean
    private AudioSpeechService audioSpeechService;

    @MockitoBean
    private AudioTranscriptionService audioTranscriptionService;

    @MockitoBean
    private Client genAiClient;

    @MockitoBean(answers = Answers.RETURNS_SELF)
    private ChatClient.Builder chatClientBuilder;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(mock(ChatClient.class));
    }

    @Test
    void shouldReturn400WhenTransactionValidationFails() throws Exception {
        when(persistTransactionUseCase.execute(any()))
                .thenThrow(new IllegalArgumentException("Valor da transação deve ser maior que zero"));

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "Compras no mercado",
                                    "category": "GROCERIES",
                                    "amount": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Valor da transação deve ser maior que zero"));
    }
}