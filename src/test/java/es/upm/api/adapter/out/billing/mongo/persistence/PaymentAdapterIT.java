package es.upm.api.adapter.out.billing.mongo.persistence;

import es.upm.api.adapter.out.billing.mongo.payment.PaymentAdapter;
import es.upm.api.adapter.out.billing.mongo.payment.PaymentEntity;
import es.upm.api.adapter.out.billing.mongo.payment.PaymentRepository;
import es.upm.api.domain.model.Payment;
import es.upm.api.domain.model.PaymentMethod;
import es.upm.api.domain.model.criteria.PaymentFindCriteria;
import es.upm.api.domain.model.external.EngagementSnapshot;
import es.upm.api.domain.model.external.UserSnapshot;
import es.upm.miw.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class PaymentAdapterIT {

    @Autowired
    private PaymentAdapter paymentAdapter;

    @MockitoBean
    private PaymentRepository paymentRepository;

    private Payment payment;
    private final PaymentFindCriteria criteria = new PaymentFindCriteria();
    private final UUID engagementId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeee000000");
    private final UUID userId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeee000001");
    private final LocalDate date = LocalDate.of(2026, 3, 20);

    @BeforeEach
    void setUp() {
        this.payment = Payment.builder()
                .id(UUID.randomUUID())
                .engagement(EngagementSnapshot.builder().id(this.engagementId).build())
                .user(UserSnapshot.builder().id(this.userId).build())
                .amount(BigDecimal.valueOf(25))
                .method(PaymentMethod.TRANSFER)
                .date(this.date)
                .build();
    }

    @Test
    void shouldCreatePayment() {
        when(this.paymentRepository.save(any(PaymentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        this.paymentAdapter.create(this.payment);

        ArgumentCaptor<PaymentEntity> captor = ArgumentCaptor.forClass(PaymentEntity.class);
        verify(this.paymentRepository).save(captor.capture());
        assertEquals(this.payment.getId(), captor.getValue().getId());
        assertEquals(this.engagementId, captor.getValue().getEngagementId());
        assertEquals(this.userId, captor.getValue().getUserId());
    }

    @Test
    void shouldReadPaymentById() {
        when(this.paymentRepository.findById(this.payment.getId()))
                .thenReturn(Optional.of(new PaymentEntity(this.payment)));

        Payment read = this.paymentAdapter.read(this.payment.getId());

        assertEquals(this.payment, read);
        verify(this.paymentRepository).findById(this.payment.getId());
    }

    @Test
    void shouldUpdatePayment() {
        UUID id = this.payment.getId();
        when(this.paymentRepository.findById(id)).thenReturn(Optional.of(new PaymentEntity(this.payment)));
        when(this.paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment update = Payment.builder()
                .engagement(EngagementSnapshot.builder().id(this.engagementId).build())
                .user(UserSnapshot.builder().id(this.userId).build())
                .amount(BigDecimal.valueOf(100))
                .method(PaymentMethod.CASH)
                .date(this.date)
                .build();

        Payment updated = this.paymentAdapter.update(id, update);
        assertEquals(BigDecimal.valueOf(100), updated.getAmount());
        assertEquals(PaymentMethod.CASH, updated.getMethod());
        verify(this.paymentRepository).save(any(PaymentEntity.class));
    }

    @Test
    void shouldDeletePayment() {
        UUID id = this.payment.getId();
        PaymentEntity entity = new PaymentEntity(this.payment);
        when(this.paymentRepository.findById(id)).thenReturn(Optional.of(entity));

        this.paymentAdapter.delete(id);

        verify(this.paymentRepository).delete(entity);
    }

    @Test
    void shouldThrowNotFoundWhenPaymentMissing() {
        UUID id = this.payment.getId();
        when(this.paymentRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> this.paymentAdapter.read(id));
        assertThrows(NotFoundException.class, () -> this.paymentAdapter.delete(id));
    }

    @Test
    void shouldFindPayments() {
        when(this.paymentRepository.findAll(PaymentAdapter.DATE))
                .thenReturn(List.of(new PaymentEntity(this.payment)));

        Stream<Payment> stream = this.paymentAdapter.find(this.criteria);

        assertEquals(this.payment, stream.findFirst().orElse(null));
        verify(this.paymentRepository).findAll(PaymentAdapter.DATE);
    }
}
