package dio.budgeting.application;

import dio.budgeting.application.input.PersistTransactionInput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersistTransactionUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Test
    void should_persist_transaction_and_return_output() {
        var useCase = new PersistTransactionUseCase(transactionRepository);
        var input = new PersistTransactionInput("Compras no mercado", 4500, Category.GROCERIES);
        var savedTransactions = new Transaction("Compras no mercado", 4500, Category.GROCERIES);

        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransactions);

        var output = useCase.execute(input);

        assertThat(output.description()).isEqualTo("Compras no mercado");
        assertThat(output.category()).isEqualTo("GROCERIES");
        assertThat(output.value()).isEqualTo(4500.0);
    }

    @Test
    void should_build_transaction_from_input_before_saving() {
        var useCase = new PersistTransactionUseCase(transactionRepository);
        var input = new PersistTransactionInput("Farmácia", 2000, Category.PHARMA);
        var captor = ArgumentCaptor.forClass(Transaction.class);

        when(transactionRepository.save(captor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(input);

        var capturedTransaction = captor.getValue();
        assertThat(capturedTransaction.getDescription()).isEqualTo("Farmácia");
        assertThat(capturedTransaction.getCategory()).isEqualTo(Category.PHARMA);
        assertThat(capturedTransaction.getAmount()).isEqualTo(2000);
    }
}