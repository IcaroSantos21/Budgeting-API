package dio.budgeting.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionTest {

    @Test
    void should_create_transaction_with_valid_data() {
        var transaction = new Transaction("Compras no mercado", 4500, Category.GROCERIES);

        assertThat(transaction.getId()).isNotNull();
        assertThat(transaction.getDescription()).isEqualTo("Compras no mercado");
        assertThat(transaction.getAmount()).isEqualTo(4500);
        assertThat(transaction.getCategory()).isEqualTo(Category.GROCERIES);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void should_reject_blank_description(String description) {
        assertThatThrownBy(() -> new Transaction(description, 4500, Category.GROCERIES))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Description cannot be null or blank");
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, -100})
    void should_reject_non_positive_amount(long amount) {
        assertThatThrownBy(() -> new Transaction("Test", amount, Category.GROCERIES))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be a positive value");
    }

    @Test
    void should_reject_null_category() {
        assertThatThrownBy(() -> new Transaction("Compras no mercado", 4500, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Category cannot be null");
    }
}