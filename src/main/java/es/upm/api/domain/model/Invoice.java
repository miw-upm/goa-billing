package es.upm.api.domain.model;

import es.upm.api.domain.model.external.EngagementSnapshot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class Invoice {
    private UUID id;
    private BillingInfo billingInfo;
    private LocalDate emissionDate;
    private LocalDate operationDate;
    private String series;
    private Integer number;
    private BigDecimal baseAmount;
    private BigDecimal vatRate;
    private EngagementSnapshot engagement;
    private List<Payment> payments;
    private List<BigDecimal> discounts;
    private String pdfPath;
    private Rectification rectification;
}
