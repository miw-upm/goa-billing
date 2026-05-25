package es.upm.api.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvoicedExpense(
        UUID expenseId,
        LocalDate issueDate,
        String description,
        BigDecimal baseAmount,
        BigDecimal vatAmount
) {}
