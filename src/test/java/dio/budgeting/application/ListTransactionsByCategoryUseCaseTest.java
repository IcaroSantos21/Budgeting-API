package dio.budgeting.application;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListTransactionsByCategoryUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Test
    void should_return_mapped_transaction_for_category() {
        var useCase = new ListTransactionsByCategoryUseCase(transactionRepository);
        var transactions = List.of(
                new Transaction("Compras no mercado", 5000, Category.GROCERIES),
                new Transaction("Feira", 2000, Category.GROCERIES)
        );

        when(transactionRepository.findAllByCategory(Category.GROCERIES)).thenReturn(transactions);

        var output = useCase.execute(Category.GROCERIES);

        assertThat(output).hasSize(2);
        assertThat(output.get(0).description()).isEqualTo("Compras no mercado");
        assertThat(output).allMatch(item -> item.category().equals("GROCERIES"));
    }

    @Test
    void should_return_empty_list_when_no_transactions_exists_for_category() {
        var useCase = new ListTransactionsByCategoryUseCase(transactionRepository);

        when(transactionRepository.findAllByCategory(Category.AUTO)).thenReturn(List.of());

        var output = useCase.execute(Category.AUTO);

        assertThat(output).isEmpty();
    }
}