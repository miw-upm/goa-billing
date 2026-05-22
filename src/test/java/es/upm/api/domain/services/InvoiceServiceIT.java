package es.upm.api.domain.services;

import es.upm.api.domain.model.BillingInfo;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.Payment;
import es.upm.api.domain.model.PaymentMethod;
import es.upm.api.domain.model.criteria.InvoiceFindCriteria;
import es.upm.api.domain.model.external.EngagementSnapshot;
import es.upm.api.domain.model.external.UserSnapshot;
import es.upm.api.domain.ports.out.billing.InvoiceGateway;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class InvoiceServiceIT {

    @Autowired
    private InvoiceService invoiceService;

    @MockitoBean
    private InvoiceGateway invoiceGateway;

    @MockitoBean
    private PaymentGateway paymentGateway;

    @MockitoBean
    private EngagementFinder engagementFinder;

    @MockitoBean
    private UserFinder userFinder;

    private Invoice invoice;
    private UUID engagementId;
    private UUID userId;
    private UUID paymentId;
    private UserSnapshot userSnapshot;
    private Payment hydratedPayment;
    private final InvoiceFindCriteria criteria = new InvoiceFindCriteria();

    @BeforeEach
    void setUp() {
        this.engagementId = UUID.randomUUID();
        this.userId = UUID.randomUUID();
        this.paymentId = UUID.randomUUID();
        this.userSnapshot = UserSnapshot.builder()
                .id(this.userId)
                .firstName("John")
                .familyName("Doe")
                .identity("12345678A")
                .address("Main St 1")
                .city("Madrid")
                .province("Madrid")
                .postalCode(28001)
                .build();
        this.hydratedPayment = Payment.builder()
                .id(this.paymentId)
                .engagement(EngagementSnapshot.builder().engagementId(this.engagementId).build())
                .user(UserSnapshot.builder().id(this.userId).build())
                .amount(BigDecimal.valueOf(100))
                .method(PaymentMethod.TRANSFER)
                .date(LocalDate.of(2026, 3, 20))
                .build();
        this.invoice = Invoice.builder()
                .billingInfo(BillingInfo.builder()
                        .userId(this.userId)
                        .fullName("John Doe")
                        .identity("12345678A")
                        .fullAddress("Madrid")
                        .build())
                .operationDate(LocalDate.of(2026, 3, 20))
                .series("A")
                .number(1)
                .baseAmount(BigDecimal.ONE)
                .engagement(EngagementSnapshot.builder().engagementId(this.engagementId).build())
                .payments(List.of(Payment.builder().id(this.paymentId).build()))
                .discounts(List.of(BigDecimal.TEN))
                .build();
    }

    @Test
    void shouldCreateInvoice() {
        when(this.engagementFinder.read(this.engagementId))
                .thenReturn(EngagementSnapshot.builder().engagementId(this.engagementId).build());
        when(this.userFinder.readById(this.userId))
                .thenReturn(this.userSnapshot);
        when(this.paymentGateway.read(this.paymentId)).thenReturn(this.hydratedPayment);

        Invoice created = this.invoiceService.create(this.invoice);

        assertNotNull(created.getId());
        assertEquals(LocalDate.now(), created.getEmissionDate());
        assertEquals(BigDecimal.valueOf(90), created.getBaseAmount());
        assertEquals(new BigDecimal("21"), created.getVatRate());
        assertEquals("John Doe", created.getBillingInfo().getFullName());
        assertEquals("12345678A", created.getBillingInfo().getIdentity());
        verify(this.engagementFinder).read(this.engagementId);
        verify(this.userFinder).readById(this.userId);
        verify(this.paymentGateway).read(this.paymentId);

        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(this.invoiceGateway).create(captor.capture());
        assertNotNull(captor.getValue().getId());
        assertEquals(BigDecimal.valueOf(90), captor.getValue().getBaseAmount());
    }

    @Test
    void shouldReadInvoice() {
        UUID id = UUID.randomUUID();
        Invoice stored = Invoice.builder()
                .id(id)
                .billingInfo(this.invoice.getBillingInfo())
                .emissionDate(LocalDate.of(2026, 3, 21))
                .operationDate(this.invoice.getOperationDate())
                .series(this.invoice.getSeries())
                .number(this.invoice.getNumber())
                .baseAmount(BigDecimal.valueOf(90))
                .vatRate(new BigDecimal("21"))
                .engagement(EngagementSnapshot.builder().engagementId(this.engagementId).build())
                .payments(List.of(this.hydratedPayment))
                .discounts(this.invoice.getDiscounts())
                .build();
        when(this.invoiceGateway.read(id)).thenReturn(stored);
        when(this.engagementFinder.read(this.engagementId)).thenReturn(stored.getEngagement());
        when(this.userFinder.readById(this.userId)).thenReturn(this.userSnapshot);

        Invoice read = this.invoiceService.read(id);

        assertEquals(id, read.getId());
        assertEquals("John Doe", read.getBillingInfo().getFullName());
        assertEquals("12345678A", read.getBillingInfo().getIdentity());
        verify(this.invoiceGateway).read(id);
        verify(this.engagementFinder).read(this.engagementId);
        verify(this.userFinder).readById(this.userId);
    }

    @Test
    void shouldUpdateInvoice() {
        UUID id = UUID.randomUUID();
        Invoice current = Invoice.builder()
                .id(id)
                .billingInfo(this.invoice.getBillingInfo())
                .emissionDate(LocalDate.of(2026, 3, 19))
                .operationDate(this.invoice.getOperationDate())
                .series("A")
                .number(1)
                .baseAmount(BigDecimal.valueOf(90))
                .vatRate(new BigDecimal("21"))
                .engagement(EngagementSnapshot.builder().engagementId(this.engagementId).build())
                .payments(List.of(this.hydratedPayment))
                .discounts(List.of(BigDecimal.TEN))
                .pdfPath("/tmp/invoice.pdf")
                .build();
        Invoice update = Invoice.builder()
                .billingInfo(this.invoice.getBillingInfo())
                .operationDate(LocalDate.of(2026, 3, 20))
                .series("B")
                .number(2)
                .baseAmount(BigDecimal.ONE)
                .engagement(EngagementSnapshot.builder().engagementId(this.engagementId).build())
                .payments(List.of(Payment.builder().id(this.paymentId).build()))
                .discounts(List.of(BigDecimal.valueOf(5)))
                .build();

        when(this.invoiceGateway.read(id)).thenReturn(current);
        when(this.engagementFinder.read(this.engagementId)).thenReturn(current.getEngagement());
        when(this.userFinder.readById(this.userId)).thenReturn(this.userSnapshot);
        when(this.paymentGateway.read(this.paymentId)).thenReturn(this.hydratedPayment);
        when(this.invoiceGateway.update(eq(id), any(Invoice.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        Invoice updated = this.invoiceService.update(id, update);

        assertEquals(id, updated.getId());
        assertEquals(current.getEmissionDate(), updated.getEmissionDate());
        assertEquals(current.getPdfPath(), updated.getPdfPath());
        assertEquals(BigDecimal.valueOf(95), updated.getBaseAmount());
        verify(this.invoiceGateway).read(id);
        verify(this.engagementFinder).read(this.engagementId);
        verify(this.userFinder).readById(this.userId);
        verify(this.paymentGateway).read(this.paymentId);
        verify(this.invoiceGateway).update(eq(id), any(Invoice.class));
    }

    @Test
    void shouldDeleteInvoice() {
        UUID id = UUID.randomUUID();
        this.invoiceService.delete(id);
        verify(this.invoiceGateway).delete(id);
    }

    @Test
    void shouldFindInvoices() {
        when(this.invoiceGateway.find(this.criteria)).thenReturn(Stream.of(this.invoice));

        Stream<Invoice> stream = this.invoiceService.find(this.criteria);

        assertEquals(this.invoice, stream.findFirst().orElse(null));
        verify(this.invoiceGateway).find(this.criteria);
        verifyNoInteractions(this.paymentGateway);
        verifyNoInteractions(this.engagementFinder);
        verifyNoInteractions(this.userFinder);
    }

    @Test
    void shouldCreateInvoiceWithoutPayments() {
        this.invoice.setPayments(List.of());
        this.invoice.setBaseAmount(BigDecimal.valueOf(123));
        when(this.engagementFinder.read(this.engagementId))
                .thenReturn(EngagementSnapshot.builder().engagementId(this.engagementId).build());
        when(this.userFinder.readById(this.userId)).thenReturn(this.userSnapshot);

        Invoice created = this.invoiceService.create(this.invoice);

        assertNotNull(created.getId());
        assertEquals(BigDecimal.valueOf(123), created.getBaseAmount());
        verify(this.invoiceGateway).create(any());
        verifyNoInteractions(this.paymentGateway);
    }

    @Test
    void shouldNotRecalculateBaseAmountWhenPaymentsAreNull() {
        this.invoice.setPayments(null);
        this.invoice.setBaseAmount(BigDecimal.valueOf(321));
        when(this.engagementFinder.read(this.engagementId))
                .thenReturn(EngagementSnapshot.builder().engagementId(this.engagementId).build());
        when(this.userFinder.readById(this.userId)).thenReturn(this.userSnapshot);

        Invoice created = this.invoiceService.create(this.invoice);

        assertEquals(BigDecimal.valueOf(321), created.getBaseAmount());
        verify(this.invoiceGateway).create(any());
        verifyNoInteractions(this.paymentGateway);
    }

    @Test
    void shouldNotCreateWhenUserFinderFails() {
        this.invoice.setPayments(List.of());
        RuntimeException exception = new RuntimeException("User not found");
        when(this.engagementFinder.read(this.engagementId))
                .thenReturn(EngagementSnapshot.builder().engagementId(this.engagementId).build());
        when(this.userFinder.readById(this.userId)).thenThrow(exception);

        try {
            this.invoiceService.create(this.invoice);
        } catch (RuntimeException ignored) {
        }

        verify(this.engagementFinder).read(this.engagementId);
        verify(this.userFinder).readById(this.userId);
        verify(this.invoiceGateway, never()).create(any());
        verifyNoInteractions(this.paymentGateway);
    }
}
