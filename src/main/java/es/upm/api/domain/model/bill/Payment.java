package es.upm.api.domain.model.bill;

import es.upm.api.domain.model.external.EngagementSnapshot;
import es.upm.api.domain.model.external.UserSnapshot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class Payment {
    private UUID id;
    private UserSnapshot user;
    private LocalDate date;
    private BigDecimal amount;
    private PaymentMethod method;
    private EngagementSnapshot engagement;
}
