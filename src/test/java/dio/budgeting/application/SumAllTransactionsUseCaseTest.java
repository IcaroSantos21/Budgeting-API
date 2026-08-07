package dio.budgeting.application;

import dio.budgeting.domain.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SumAllTransactionsUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Test
    void should_return_total_sum_of_all_transactions() {
        var useCase = new SumAllTransactionsUseCase(transactionRepository);

        when(transactionRepository.sumAmountTotal()).thenReturn(15000L);

        var output = useCase.execute();

        assertThat(output.total()).isEqualTo(15000.0);
    }

    @Test
    void should_return_zero_when_no_transactions_exists() {
        var useCase = new SumAllTransactionsUseCase(transactionRepository);

        when(transactionRepository.sumAmountTotal()).thenReturn(0L);

        var output = useCase.execute();

        assertThat(output.total()).isEqualTo(0.0);

    }
}