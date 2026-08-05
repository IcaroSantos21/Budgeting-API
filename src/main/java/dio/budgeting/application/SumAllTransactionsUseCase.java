package dio.budgeting.application;

import dio.budgeting.application.output.TransactionsSumOutput;
import dio.budgeting.domain.TransactionRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class SumAllTransactionsUseCase {

    private final TransactionRepository transactionRepository;

    public SumAllTransactionsUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Tool(name = "sum-all-transactions", description = "Soma o total de todas as transações financeiras")
    public TransactionsSumOutput execute() {
        return new TransactionsSumOutput(transactionRepository.sumAmountTotal());
    }
}
