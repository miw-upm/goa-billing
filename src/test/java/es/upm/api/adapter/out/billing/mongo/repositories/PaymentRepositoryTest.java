package es.upm.api.adapter.out.billing.mongo.repositories;

import es.upm.api.adapter.out.billing.mongo.payment.PaymentEntity;
import es.upm.api.adapter.out.billing.mongo.payment.PaymentRepository;
import es.upm.api.domain.model.Payment;
import es.upm.api.domain.model.PaymentMethod;
import es.upm.api.domain.model.external.EngagementSnapshot;
import es.upm.api.domain.model.external.UserSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataMongoTest
@ActiveProfiles("test")
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    private Payment payment;

    @BeforeEach
    void setUp() {
        this.paymentRepository.deleteAll();
        this.payment = Payment.builder()
                .id(UUID.randomUUID())
                .engagement(EngagementSnapshot.builder().id(UUID.randomUUID()).build())
                .user(UserSnapshot.builder().id(UUID.randomUUID()).build())
                .amount(BigDecimal.valueOf(300))
                .method(PaymentMethod.TRANSFER)
                .date(LocalDate.of(2026, 3, 20))
                .build();
    }

    @Test
    void shouldSavePayment() {
        PaymentEntity saved = this.paymentRepository.save(new PaymentEntity(this.payment));

        assertNotNull(saved);
        assertEquals(this.payment.getId(), saved.getId());
        assertEquals(this.payment.getEngagement().getId(), saved.getEngagementId());
        assertEquals(this.payment.getUser().getId(), saved.getUserId());
        assertEquals(this.payment.getAmount(), saved.getAmount());
        assertEquals(this.payment.getMethod(), saved.getMethod());
        assertEquals(this.payment.getDate(), saved.getDate());
    }

    @Test
    void shouldFindPaymentById() {
        PaymentEntity saved = this.paymentRepository.save(new PaymentEntity(this.payment));
        Optional<PaymentEntity> optional = this.paymentRepository.findById(saved.getId());

        assertTrue(optional.isPresent());
        assertEquals(saved.getId(), optional.get().getId());
        assertEquals(saved.getEngagementId(), optional.get().getEngagementId());
        assertEquals(saved.getUserId(), optional.get().getUserId());
    }

    @Test
    void shouldFindPaymentsFromDate() {
        Payment older = Payment.builder()
                .id(UUID.randomUUID())
                .engagement(EngagementSnapshot.builder().id(UUID.randomUUID()).build())
                .user(UserSnapshot.builder().id(UUID.randomUUID()).build())
                .amount(BigDecimal.valueOf(100))
                .method(PaymentMethod.CASH)
                .date(LocalDate.of(2026, 3, 18))
                .build();
        Payment newer = Payment.builder()
                .id(UUID.randomUUID())
                .engagement(EngagementSnapshot.builder().id(UUID.randomUUID()).build())
                .user(UserSnapshot.builder().id(UUID.randomUUID()).build())
                .amount(BigDecimal.valueOf(200))
                .method(PaymentMethod.TRANSFER)
                .date(LocalDate.of(2026, 3, 20))
                .build();
        this.paymentRepository.save(new PaymentEntity(older));
        this.paymentRepository.save(new PaymentEntity(newer));

        List<PaymentEntity> result = this.paymentRepository
                .findByDateGreaterThanEqualOrderByDateDesc(LocalDate.of(2026, 3, 19));

        assertEquals(1, result.size());
        assertEquals(newer.getId(), result.get(0).getId());
    }
}
