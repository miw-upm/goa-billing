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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataMongoTest
@ActiveProfiles("test")
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    private Payment currentPayment;
    private Payment olderPayment;
    private UUID engagementId;

    @BeforeEach
    void setUp() {
        this.paymentRepository.deleteAll();
        this.engagementId = UUID.randomUUID();
        this.olderPayment = Payment.builder()
                .id(UUID.randomUUID())
                .engagement(EngagementSnapshot.builder().id(this.engagementId).build())
                .user(UserSnapshot.builder().id(UUID.randomUUID()).build())
                .amount(BigDecimal.valueOf(100))
                .method(PaymentMethod.CASH)
                .date(LocalDate.of(2026, 3, 18))
                .invoiced(false)
                .build();
        this.currentPayment = Payment.builder()
                .id(UUID.randomUUID())
                .engagement(EngagementSnapshot.builder().id(this.engagementId).build())
                .user(UserSnapshot.builder().id(UUID.randomUUID()).build())
                .amount(BigDecimal.valueOf(200))
                .method(PaymentMethod.TRANSFER)
                .date(LocalDate.of(2026, 3, 20))
                .invoiced(true)
                .build();
        this.paymentRepository.saveAll(List.of(
                new PaymentEntity(this.olderPayment),
                new PaymentEntity(this.currentPayment)
        ));
    }

    @Test
    void shouldFindPaymentsFromDate() {
        List<PaymentEntity> result = this.paymentRepository
                .findByDateGreaterThanEqualOrderByDateDesc(LocalDate.of(2026, 3, 19));

        assertEquals(1, result.size());
        assertEquals(this.currentPayment.getId().toString(), result.getFirst().getId());
    }

    @Test
    void shouldFindByEngagementIdPrefixAndDateAndInvoiced() {
        String engagementPrefix = this.engagementId.toString().substring(0, 4);
        List<PaymentEntity> result = this.paymentRepository
                .findByEngagementIdStartingWithAndDateGreaterThanEqualAndInvoicedOrderByDateDesc(
                        engagementPrefix, LocalDate.of(2026, 3, 19), true
                );

        assertEquals(1, result.size());
        assertEquals(this.currentPayment.getId().toString(), result.getFirst().getId());
    }

    @Test
    void shouldFindNotInvoicedByEngagementId() {
        List<PaymentEntity> result = this.paymentRepository
                .findByEngagementIdAndInvoicedFalseOrderByDateDesc(engagementId.toString());

        assertEquals(1, result.size());
        assertEquals(this.olderPayment.getId().toString(), result.getFirst().getId());
    }
}
