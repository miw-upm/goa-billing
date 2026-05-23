package es.upm.api.domain.services;

import es.upm.api.domain.model.Payment;
import es.upm.api.domain.model.PaymentMethod;
import es.upm.api.domain.model.criteria.PaymentFindCriteria;
import es.upm.api.domain.model.external.EngagementSnapshot;
import es.upm.api.domain.model.external.UserSnapshot;
import es.upm.api.domain.ports.out.billing.PaymentGateway;
import es.upm.api.domain.ports.out.engagement.EngagementFinder;
import es.upm.api.domain.ports.out.user.UserFinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class PaymentServiceIT {
    @Autowired
    private PaymentService paymentService;

    @MockitoBean
    private PaymentGateway paymentGateway;

    @MockitoBean
    private EngagementFinder engagementFinder;

    @MockitoBean
    private UserFinder userFinder;

    private Payment payment;
    private UUID engagementId;
    private UUID userId;
    private final PaymentFindCriteria criteria = new PaymentFindCriteria();

    @BeforeEach
    void setUp() {
        this.engagementId = UUID.randomUUID();
        this.userId = UUID.randomUUID();
        this.payment = Payment.builder()
                .engagement(EngagementSnapshot.builder().id(this.engagementId).build())
                .user(UserSnapshot.builder().id(this.userId).build())
                .amount(BigDecimal.valueOf(250))
                .method(PaymentMethod.TRANSFER)
                .date(LocalDate.of(2026, 3, 20))
                .build();
    }


    @Test
    void shouldReadPayment() {
        UUID id = UUID.randomUUID();
        this.payment.setId(id);
        when(this.paymentGateway.read(id)).thenReturn(this.payment);
        when(this.engagementFinder.read(this.engagementId)).thenReturn(this.payment.getEngagement());
        when(this.userFinder.readById(this.userId)).thenReturn(this.payment.getUser());

        Payment read = this.paymentService.read(id);

        assertEquals(id, read.getId());
        verify(this.paymentGateway).read(id);
        verify(this.engagementFinder).read(this.engagementId);
        verify(this.userFinder).readById(this.userId);
    }



    @Test
    void shouldDeletePayment() {
        UUID id = UUID.randomUUID();
        this.paymentService.delete(id);
        verify(this.paymentGateway).delete(id);
    }

    @Test
    void shouldNotCreateWhenUserFinderFails() {
        RuntimeException exception = new RuntimeException("User not found");
        when(this.engagementFinder.read(this.engagementId))
                .thenReturn(EngagementSnapshot.builder().id(this.engagementId).build());
        when(this.userFinder.readById(this.userId)).thenThrow(exception);

        try {
            this.paymentService.create(this.payment);
        } catch (RuntimeException ignored) {
        }

        verify(this.engagementFinder).read(this.engagementId);
        verify(this.userFinder).readById(this.userId);
        verify(this.paymentGateway, never()).create(any());
    }
}
