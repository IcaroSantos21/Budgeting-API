package dio.budgeting.application;

import dio.budgeting.application.output.TransactionsSumOutput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.TransactionRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class SumTransactionsByCategoryUseCase {

    private final TransactionRepository transactionRepository;

    public SumTransactionsByCategoryUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Tool(name = "sum-transactions-by-category", description = "Soma o total de transações financeiras de uma determinada categoria")
    public TransactionsSumOutput execute(@ToolParam(description = "Categoria de uma transação") Category category) {
        return new TransactionsSumOutput(transactionRepository.sumAmountByCategory(category));
    }
}
