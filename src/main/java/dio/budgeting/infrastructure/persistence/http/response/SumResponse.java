package dio.budgeting.infrastructure.persistence.http.response;

import dio.budgeting.application.output.TransactionsSumOutput;

public record SumResponse(double total) {

    public static SumResponse from(TransactionsSumOutput output) {
        return new SumResponse(output.total());
    }
}
