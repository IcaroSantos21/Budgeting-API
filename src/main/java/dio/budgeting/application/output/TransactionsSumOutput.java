package dio.budgeting.application.output;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record TransactionsSumOutput(double total) {

    public static TransactionsSumOutput from(long amount) {
        return new TransactionsSumOutput(
                BigDecimal.valueOf(amount)
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue()
        );
    }
}
