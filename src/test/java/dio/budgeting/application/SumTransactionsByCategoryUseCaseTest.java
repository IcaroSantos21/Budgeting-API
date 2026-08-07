package dio.budgeting.application;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SumTransactionsByCategoryUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Test
    void should_return_sum_for_category_with_transactions() {

        var useCase = new SumTransactionsByCategoryUseCase(transactionRepository);

        when(transactionRepository.sumAmountByCategory(Category.GROCERIES)).thenReturn(7000L);

        var output = useCase.execute(Category.GROCERIES);

        assertThat(output.total()).isEqualTo(7000.0);
    }

    @Test
    void should_return_zero_for_category_without_transactions() {

        var useCase = new SumTransactionsByCategoryUseCase(transactionRepository);

        when(transactionRepository.sumAmountByCategory(Category.AUTO)).thenReturn(0L);

        var output = useCase.execute(Category.AUTO);

        assertThat(output.total()).isEqualTo(0.0);
    }
}