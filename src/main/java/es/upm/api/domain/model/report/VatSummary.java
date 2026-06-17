package es.upm.api.domain.model.report;

import java.math.BigDecimal;

public record VatSummary(
        BigDecimal invoiceIssuedBase,
        BigDecimal invoiceIssuedVat,
        BigDecimal invoiceReceivedCurrentBase,
        BigDecimal invoiceReceivedCurrentVat,
        BigDecimal invoiceReceivedInvestmentBase,
        BigDecimal invoiceReceivedInvestmentVat
) {
}
