package dio.budgeting.application;

import dio.budgeting.application.output.TransactionsSumOutput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.TransactionRepository;
import org.springframework.stereotype.Service;

@Service
public class SumTransactionsByCategoryUseCase {

    private final TransactionRepository transactionRepository;

    public SumTransactionsByCategoryUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public TransactionsSumOutput execute(Category category) {
        return new TransactionsSumOutput(transactionRepository.sumAmountByCategory(category));
    }
}
