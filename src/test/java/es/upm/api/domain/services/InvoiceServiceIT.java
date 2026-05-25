package es.upm.api.domain.services;

import es.upm.api.domain.model.BillingInfo;
import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.Payment;
import es.upm.api.domain.model.PaymentMethod;
import es.upm.api.domain.model.criteria.InvoiceFindCriteria;
import es.upm.api.domain.model.external.EngagementSnapshot;
import es.upm.api.domain.model.external.UserSnapshot;
import es.upm.api.domain.ports.out.billing.ExpenseGateway;
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
    private ExpenseGateway expenseGateway;

    @MockitoBean
    private EngagementFinder engagementFinder;

    @MockitoBean
    private UserFinder userFinder;

    private UUID userId;
    private UUID engagementId;
    private UserSnapshot userSnapshot;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        this.userId = UUID.randomUUID();
        this.engagementId = UUID.randomUUID();
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
        this.invoice = Invoice.builder()
                .billingInfo(BillingInfo.builder()
                        .userId(this.userId)
                        .concept("Servicios")
                        .build())
                .baseAmount(new BigDecimal("100.00"))
                .build();
    }

    @Test
    void shouldCreateInvoiceHydratingBillingInfo() {
        when(this.userFinder.readById(this.userId)).thenReturn(this.userSnapshot);

        this.invoiceService.create(this.invoice);

        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(this.invoiceGateway).create(captor.capture());
        assertNotNull(captor.getValue().getId());
        assertEquals(this.userId, captor.getValue().getBillingInfo().getUserId());
        assertEquals("John Doe", captor.getValue().getBillingInfo().getFullName());
        assertEquals("Servicios", captor.getValue().getBillingInfo().getConcept());
    }

    @Test
    void shouldUpdateInvoice() {
        UUID invoiceId = UUID.randomUUID();
        Invoice current = Invoice.builder()
                .id(invoiceId)
                .billingInfo(BillingInfo.builder()
                        .userId(this.userId)
                        .fullName("John Doe")
                        .identity("12345678A")
                        .fullAddress("Main St 1, Madrid, Madrid, 28001")
                        .build())
                .emissionDate(null)
                .pdfPath("/tmp/invoice.pdf")
                .baseAmount(new BigDecimal("100.00"))
                .engagement(EngagementSnapshot.builder().id(this.engagementId).build())
                .build();
        Invoice request = Invoice.builder()
                .billingInfo(BillingInfo.builder()
                        .userId(this.userId)
                        .concept("Actualizado")
                        .build())
                .baseAmount(new BigDecimal("150.00"))
                .engagement(EngagementSnapshot.builder().id(this.engagementId).build())
                .build();

        when(this.invoiceGateway.read(invoiceId)).thenReturn(current);
        when(this.userFinder.readById(this.userId)).thenReturn(this.userSnapshot);
        when(this.engagementFinder.read(this.engagementId)).thenReturn(current.getEngagement());
        when(this.invoiceGateway.update(eq(invoiceId), any(Invoice.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        Invoice updated = this.invoiceService.update(invoiceId, request);

        assertEquals(invoiceId, updated.getId());
        assertEquals("/tmp/invoice.pdf", updated.getPdfPath());
        assertEquals("Actualizado", updated.getBillingInfo().getConcept());
        verify(this.invoiceGateway).update(eq(invoiceId), any(Invoice.class));
    }

    @Test
    void shouldDeleteInvoiceOnlyWhenDraft() {
        UUID invoiceId = UUID.randomUUID();

        this.invoiceService.delete(invoiceId);

        verify(this.invoiceGateway).delete(invoiceId);
    }

    @Test
    void shouldFindFilteringByClient() {
        UUID invoiceId = UUID.randomUUID();
        Invoice first = Invoice.builder()
                .id(invoiceId)
                .billingInfo(BillingInfo.builder().userId(this.userId).build())
                .baseAmount(BigDecimal.TEN)
                .build();
        Invoice second = Invoice.builder()
                .id(UUID.randomUUID())
                .billingInfo(BillingInfo.builder().userId(UUID.randomUUID()).build())
                .baseAmount(BigDecimal.ONE)
                .build();
        InvoiceFindCriteria criteria = new InvoiceFindCriteria("john", LocalDate.of(2026, 3, 20));
        when(this.invoiceGateway.find(criteria)).thenReturn(Stream.of(first, second));
        when(this.userFinder.find("john")).thenReturn(List.of(this.userSnapshot));

        List<Invoice> invoices = this.invoiceService.find(criteria).toList();

        assertEquals(1, invoices.size());
        assertEquals(invoiceId, invoices.get(0).getId());
    }

    @Test
    void shouldFindWithoutClientFilter() {
        InvoiceFindCriteria criteria = new InvoiceFindCriteria(null, LocalDate.of(2026, 3, 20));
        when(this.invoiceGateway.find(criteria)).thenReturn(Stream.of(this.invoice));

        List<Invoice> invoices = this.invoiceService.find(criteria).toList();

        assertEquals(1, invoices.size());
        verify(this.userFinder, never()).find(any());
    }

 }
