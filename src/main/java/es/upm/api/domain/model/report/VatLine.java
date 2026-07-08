package es.upm.api.domain.model.report;

import java.math.BigDecimal;

public record VatLine(BigDecimal baseAmount, BigDecimal vatAmount, BigDecimal totalAmount) {}
