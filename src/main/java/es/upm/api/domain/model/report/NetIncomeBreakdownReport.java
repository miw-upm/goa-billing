package es.upm.api.domain.model.report;

import java.math.BigDecimal;

public record NetIncomeBreakdownReport(
        BigDecimal income,
        BigDecimal currentExpenses,
        BigDecimal investmentAmortization,
        BigDecimal withholdings
) {
}
