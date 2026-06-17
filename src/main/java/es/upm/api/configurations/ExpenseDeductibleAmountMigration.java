package es.upm.api.configurations;

import es.upm.api.adapter.out.billing.mongo.expense.ExpenseEntity;
import es.upm.api.adapter.out.billing.mongo.expense.ExpenseRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Log4j2
@Component
public class ExpenseDeductibleAmountMigration implements ApplicationRunner {

    private static final BigDecimal DEFAULT_DEDUCTIBLE_AMOUNT = new BigDecimal("100");

    private final ExpenseRepository expenseRepository;

    public ExpenseDeductibleAmountMigration(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<ExpenseEntity> expenses = this.expenseRepository.findAll().stream()
                .filter(expense -> expense.getDeductibleAmount() == null)
                .toList();
        if (!expenses.isEmpty()) {
            expenses.forEach(expense -> expense.setDeductibleAmount(DEFAULT_DEDUCTIBLE_AMOUNT));
            log.warn("MIGRATION: setting deductibleAmount=100 on {} expenses with null deductibleAmount",
                    expenses.size());
            this.expenseRepository.saveAll(expenses);
        }
    }
}
