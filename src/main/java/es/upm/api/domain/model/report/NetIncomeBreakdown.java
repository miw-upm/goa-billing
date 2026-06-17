package es.upm.api.domain.model.report;

import java.math.BigDecimal;

public record NetIncomeBreakdown(
        BigDecimal income,
        BigDecimal currentExpenses,
        BigDecimal investmentAmortization,
        BigDecimal withholdings
) {
}
