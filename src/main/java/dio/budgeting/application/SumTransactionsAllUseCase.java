package dio.budgeting.application;

import dio.budgeting.application.output.TransactionsSumOutput;
import dio.budgeting.domain.TransactionRepository;
import org.springframework.stereotype.Service;

@Service
public class SumTransactionsAllUseCase {

    private final TransactionRepository transactionRepository;

    public SumTransactionsAllUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public TransactionsSumOutput execute() {
        return new TransactionsSumOutput(transactionRepository.sumAmountTotal());
    }
}
