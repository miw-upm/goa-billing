package es.upm.api.domain.model;

import es.upm.api.domain.model.external.UserSnapshot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvoicedPayment(
    UUID paymentId,
    LocalDate date,
    BigDecimal amount,
    PaymentMethod method,
    UserSnapshot user
){}
