package es.upm.api.domain.model.report;

import java.math.BigDecimal;

public record Model303(
        int year,
        int quarter,
        BigDecimal accruedBase,
        BigDecimal accruedQuota,
        BigDecimal currentOperationsBase,
        BigDecimal currentOperationsQuota,
        BigDecimal investmentAssetsBase,
        BigDecimal investmentAssetsQuota
) {}
