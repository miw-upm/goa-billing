package es.upm.api.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

public record InvoicedExpense(
        UUID expenseId,
        LocalDate issueDate,
        String description,
        BigDecimal baseAmount,
        BigDecimal vatAmount
) {
    public InvoicedExpense(Expense expense) {
        this(
                expense.getId(),
                expense.getIssueDate(),
                expense.getDescription(),
                expense.getBaseAmount(),
                expense.getBaseAmount()
                        .multiply(BigDecimal.valueOf(expense.getVatRate()))
                        .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
        );
    }
}
