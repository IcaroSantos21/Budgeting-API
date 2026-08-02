package dio.budgeting.infrastructure.persistence.http.response;

import dio.budgeting.application.output.TransactionOutput;

public record TransactionResponse(String id, String description, String category, double amount) {
    public static TransactionResponse from(TransactionOutput output) {
        return new TransactionResponse(
                output.id(),
                output.description(),
                output.category(),
                output.value()
        );
    }
}
