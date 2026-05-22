package es.upm.api.adapter.out.billing.mongo.entities;

import es.upm.api.adapter.out.billing.mongo.payment.PaymentEntity;
import es.upm.api.domain.model.Payment;
import es.upm.api.domain.model.PaymentMethod;
import es.upm.api.domain.model.external.EngagementSnapshot;
import es.upm.api.domain.model.external.UserSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentEntityTest {

    private Payment payment;

    @BeforeEach
    void setUp() {
        this.payment = Payment.builder()
                .id(UUID.randomUUID())
                .engagement(EngagementSnapshot.builder().id(UUID.randomUUID()).build())
                .user(UserSnapshot.builder().id(UUID.randomUUID()).build())
                .amount(BigDecimal.valueOf(250))
                .method(PaymentMethod.BIZUM)
                .date(LocalDate.of(2026, 3, 20))
                .build();
    }

    @Test
    void shouldBuildPaymentEntityFromPayment() {
        PaymentEntity paymentEntity = new PaymentEntity(this.payment);

        assertEquals(this.payment.getId(), paymentEntity.getId());
        assertEquals(this.payment.getEngagement().getId(), paymentEntity.getEngagementId());
        assertEquals(this.payment.getUser().getId(), paymentEntity.getUserId());
        assertEquals(this.payment.getAmount(), paymentEntity.getAmount());
        assertEquals(this.payment.getMethod(), paymentEntity.getMethod());
        assertEquals(this.payment.getDate(), paymentEntity.getDate());
    }

    @Test
    void shouldConvertPaymentEntityToDomain() {
        PaymentEntity paymentEntity = new PaymentEntity();
        paymentEntity.setId(this.payment.getId());
        paymentEntity.setEngagementId(this.payment.getEngagement().getId());
        paymentEntity.setUserId(this.payment.getUser().getId());
        paymentEntity.setAmount(this.payment.getAmount());
        paymentEntity.setMethod(this.payment.getMethod());
        paymentEntity.setDate(this.payment.getDate());

        Payment mapped = paymentEntity.toDomain();

        assertEquals(paymentEntity.getId(), mapped.getId());
        assertEquals(paymentEntity.getEngagementId(), mapped.getEngagement().getId());
        assertEquals(paymentEntity.getUserId(), mapped.getUser().getId());
        assertEquals(paymentEntity.getAmount(), mapped.getAmount());
        assertEquals(paymentEntity.getMethod(), mapped.getMethod());
        assertEquals(paymentEntity.getDate(), mapped.getDate());
    }
}
