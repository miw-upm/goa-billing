package es.upm.api.adapter.out.billing.mongo.persistence;

import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceAdapter;
import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceEntity;
import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceRepository;
import es.upm.api.domain.model.BillingInfo;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.Payment;
import es.upm.api.domain.model.PaymentMethod;
import es.upm.api.domain.model.criteria.InvoiceFindCriteria;
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
class InvoiceAdapterIT {

    @Autowired
    private InvoiceAdapter invoiceAdapter;

    @MockitoBean
    private InvoiceRepository invoiceRepository;

    private Invoice invoice;
    private UUID engagementId;
    private UUID userId;
    private UUID paymentId;
    private final InvoiceFindCriteria criteria = new InvoiceFindCriteria();

    @BeforeEach
    void setUp() {
        this.engagementId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeee111000");
        this.userId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeee111001");
        this.paymentId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeee111002");
        this.invoice = this.buildInvoice(UUID.randomUUID(), this.engagementId, this.userId, this.paymentId,
                LocalDate.of(2026, 3, 20), BigDecimal.valueOf(90));
    }

    @Test
    void shouldCreateInvoice() {
        when(this.invoiceRepository.save(any(InvoiceEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        this.invoiceAdapter.create(this.invoice);

        ArgumentCaptor<InvoiceEntity> captor = ArgumentCaptor.forClass(InvoiceEntity.class);
        verify(this.invoiceRepository).save(captor.capture());
        assertEquals(this.invoice.getId(), captor.getValue().getId());
        assertEquals(this.invoice.getEngagement().getId(), captor.getValue().getEngagementId());
        assertEquals(this.invoice.getBaseAmount(), captor.getValue().getBaseAmount());
    }

    @Test
    void shouldReadInvoiceById() {
        when(this.invoiceRepository.findById(this.invoice.getId()))
                .thenReturn(Optional.of(new InvoiceEntity(this.invoice)));

        Invoice read = this.invoiceAdapter.read(this.invoice.getId());

        assertEquals(this.invoice, read);
        verify(this.invoiceRepository).findById(this.invoice.getId());
    }

    @Test
    void shouldUpdateInvoice() {
        UUID id = this.invoice.getId();
        when(this.invoiceRepository.findById(id)).thenReturn(Optional.of(new InvoiceEntity(this.invoice)));
        when(this.invoiceRepository.save(any(InvoiceEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Invoice update = this.buildInvoice(id, this.engagementId, this.userId, this.paymentId,
                LocalDate.of(2026, 3, 21), BigDecimal.valueOf(120));
        update.setSeries("B");
        update.setNumber(2);

        Invoice updated = this.invoiceAdapter.update(id, update);

        assertEquals(BigDecimal.valueOf(120), updated.getBaseAmount());
        assertEquals("B", updated.getSeries());
        assertEquals(2, updated.getNumber());
        verify(this.invoiceRepository).save(any(InvoiceEntity.class));
    }

    @Test
    void shouldDeleteInvoice() {
        UUID id = this.invoice.getId();
        InvoiceEntity entity = new InvoiceEntity(this.invoice);
        when(this.invoiceRepository.findById(id)).thenReturn(Optional.of(entity));

        this.invoiceAdapter.delete(id);

        verify(this.invoiceRepository).delete(entity);
    }

    @Test
    void shouldThrowNotFoundWhenInvoiceMissing() {
        UUID id = this.invoice.getId();
        when(this.invoiceRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> this.invoiceAdapter.read(id));
        assertThrows(NotFoundException.class, () -> this.invoiceAdapter.update(id, this.invoice));
        assertThrows(NotFoundException.class, () -> this.invoiceAdapter.delete(id));
    }

    @Test
    void shouldFindInvoicesWithoutFilters() {
        when(this.invoiceRepository.findAll(InvoiceAdapter.DATE))
                .thenReturn(List.of(new InvoiceEntity(this.invoice)));

        Stream<Invoice> stream = this.invoiceAdapter.find(this.criteria);

        assertEquals(this.invoice, stream.findFirst().orElse(null));
        verify(this.invoiceRepository).findAll(InvoiceAdapter.DATE);
    }

    @Test
    void shouldFindInvoicesByEngagementAndDate() {
        InvoiceFindCriteria findCriteria = new InvoiceFindCriteria(this.engagementId, LocalDate.of(2026, 3, 20));
        Invoice second = this.buildInvoice(UUID.randomUUID(), this.engagementId, this.userId, this.paymentId,
                LocalDate.of(2026, 3, 19), BigDecimal.valueOf(40));
        when(this.invoiceRepository.findByEngagementId(this.engagementId))
                .thenReturn(List.of(new InvoiceEntity(this.invoice), new InvoiceEntity(second)));

        List<Invoice> result = this.invoiceAdapter.find(findCriteria).toList();

        assertEquals(1, result.size());
        assertEquals(this.invoice.getId(), result.get(0).getId());
        verify(this.invoiceRepository).findByEngagementId(this.engagementId);
    }

    private Invoice buildInvoice(UUID invoiceId, UUID engagementId, UUID userId, UUID paymentId,
                                 LocalDate emissionDate, BigDecimal baseAmount) {
        return Invoice.builder()
                .id(invoiceId)
                .billingInfo(BillingInfo.builder()
                        .userId(userId)
                        .fullName("John Doe")
                        .identity("12345678A")
                        .fullAddress("Madrid")
                        .build())
                .emissionDate(emissionDate)
                .operationDate(emissionDate.minusDays(1))
                .series("A")
                .number(1)
                .baseAmount(baseAmount)
                .vatRate(BigDecimal.valueOf(21))
                .engagement(EngagementSnapshot.builder().id(engagementId).build())
                .payments(List.of(Payment.builder()
                        .id(paymentId)
                        .engagement(EngagementSnapshot.builder().id(engagementId).build())
                        .user(UserSnapshot.builder().id(userId).build())
                        .amount(baseAmount.add(BigDecimal.TEN))
                        .method(PaymentMethod.TRANSFER)
                        .date(emissionDate.minusDays(2))
                        .build()))
                .discounts(List.of(BigDecimal.TEN))
                .pdfPath("/tmp/invoice.pdf")
                .build();
    }
}
