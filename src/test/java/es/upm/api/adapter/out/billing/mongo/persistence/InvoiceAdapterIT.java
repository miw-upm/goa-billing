package es.upm.api.adapter.out.billing.mongo.persistence;

import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceAdapter;
import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceEntity;
import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceRepository;
import es.upm.api.domain.model.*;
import es.upm.api.domain.model.creation.InvoiceLegalProcedure;
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

    private final InvoiceFindCriteria criteria = new InvoiceFindCriteria();
    @Autowired
    private InvoiceAdapter invoiceAdapter;
    @MockitoBean
    private InvoiceRepository invoiceRepository;
    private Invoice invoice;
    private UUID engagementId;
    private UUID userId;
    private UUID paymentId;

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
        assertEquals(this.invoice.getConcept(), captor.getValue().getConcept());
        assertEquals(this.invoice.getClosed(), captor.getValue().getClosed());
        assertEquals(this.invoice.getEngagement().getId(), captor.getValue().getEngagementId());
        assertEquals(this.invoice.getLegalProcedures(), captor.getValue().getLegalProcedures());
        assertEquals(this.invoice.getBaseAmount(), captor.getValue().getBaseAmount());
        assertEquals(this.invoice.getPriorPayments(), captor.getValue().getPriorPayments());
        assertEquals(this.invoice.getExpenses(), captor.getValue().getExpenses());
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
        update.setConcept("Servicios 2");
        update.setClosed(true);
        update.setSeries("B");
        update.setNumber(2);

        Invoice updated = this.invoiceAdapter.update(id, update);

        assertEquals(BigDecimal.valueOf(120), updated.getBaseAmount());
        assertEquals("Servicios 2", updated.getConcept());
        assertEquals(Boolean.TRUE, updated.getClosed());
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
        when(this.invoiceRepository.findAllByOrderByEmissionDateDesc())
                .thenReturn(List.of(new InvoiceEntity(this.invoice)));

        Stream<Invoice> stream = this.invoiceAdapter.find(this.criteria);

        assertEquals(this.invoice, stream.findFirst().orElse(null));
        verify(this.invoiceRepository).findAllByOrderByEmissionDateDesc();
    }

    @Test
    void shouldFindInvoicesByFromDate() {
        LocalDate fromDate = LocalDate.of(2026, 3, 20);
        InvoiceFindCriteria findCriteria = new InvoiceFindCriteria(null, fromDate);
        when(this.invoiceRepository.findByEmissionDateGreaterThanEqualOrderByEmissionDateDesc(fromDate))
                .thenReturn(List.of(new InvoiceEntity(this.invoice)));

        List<Invoice> result = this.invoiceAdapter.find(findCriteria).toList();

        assertEquals(1, result.size());
        assertEquals(this.invoice.getId(), result.get(0).getId());
        verify(this.invoiceRepository).findByEmissionDateGreaterThanEqualOrderByEmissionDateDesc(fromDate);
    }

    @Test
    void shouldFindInvoicesByEngagementReference() {
        String encodedEngagementId = InvoiceEntity.encodeEngagementId(this.engagementId);
        InvoiceFindCriteria findCriteria = new InvoiceFindCriteria();
        findCriteria.setEngagementReference(encodedEngagementId.substring(0, 4));
        when(this.invoiceRepository.findByEngagementIdCode64StartingWithOrderByEmissionDateDesc(encodedEngagementId.substring(0, 4)))
                .thenReturn(List.of(new InvoiceEntity(this.invoice)));

        List<Invoice> result = this.invoiceAdapter.find(findCriteria).toList();

        assertEquals(1, result.size());
        assertEquals(this.invoice.getId(), result.get(0).getId());
        verify(this.invoiceRepository).findByEngagementIdCode64StartingWithOrderByEmissionDateDesc(encodedEngagementId.substring(0, 4));
    }

    @Test
    void shouldFindInvoicesByEngagementReferenceAndFromDate() {
        LocalDate fromDate = LocalDate.of(2026, 3, 20);
        String encodedEngagementId = InvoiceEntity.encodeEngagementId(this.engagementId);
        InvoiceFindCriteria findCriteria = new InvoiceFindCriteria();
        findCriteria.setFromDate(fromDate);
        findCriteria.setEngagementReference(encodedEngagementId.substring(0, 4));
        when(this.invoiceRepository.findByEngagementIdCode64StartingWithAndEmissionDateGreaterThanEqualOrderByEmissionDateDesc(
                encodedEngagementId.substring(0, 4), fromDate))
                .thenReturn(List.of(new InvoiceEntity(this.invoice)));

        List<Invoice> result = this.invoiceAdapter.find(findCriteria).toList();

        assertEquals(1, result.size());
        assertEquals(this.invoice.getId(), result.get(0).getId());
        verify(this.invoiceRepository).findByEngagementIdCode64StartingWithAndEmissionDateGreaterThanEqualOrderByEmissionDateDesc(
                encodedEngagementId.substring(0, 4), fromDate);
    }

    private Invoice buildInvoice(UUID invoiceId, UUID engagementId, UUID userId, UUID paymentId,
                                 LocalDate emissionDate, BigDecimal baseAmount) {
        return Invoice.builder()
                .id(invoiceId)
                .concept("Servicios")
                .closed(false)
                .billingInfo(BillingInfo.builder()
                        .userId(userId)
                        .fullName("John Doe")
                        .identity("12345678A")
                        .fullAddress("Madrid")
                        .build())
                .percentage(new BigDecimal("100"))
                .emissionDate(emissionDate)
                .operationDate(emissionDate.minusDays(1))
                .series("A")
                .number(1)
                .baseAmount(baseAmount)
                .vatAmount(new BigDecimal("18.90"))
                .vatRate(BigDecimal.valueOf(21))
                .engagement(EngagementSnapshot.builder().id(engagementId).build())
                .legalProcedures(List.of(InvoiceLegalProcedure.builder()
                        .title("Procedimiento")
                        .budget(baseAmount)
                        .legalTasks(List.of("Tarea 1"))
                        .build()))
                .payments(List.of(new InvoicedPayment(
                        paymentId,
                        emissionDate.minusDays(2),
                        baseAmount.add(BigDecimal.TEN),
                        PaymentMethod.TRANSFER,
                        UserSnapshot.builder().id(userId).build()
                )))
                .priorPayments(List.of(new InvoicedPayment(
                        UUID.randomUUID(),
                        emissionDate.minusDays(5),
                        BigDecimal.TEN,
                        PaymentMethod.CASH,
                        UserSnapshot.builder().id(userId).build()
                )))
                .expenses(List.of(new InvoicedExpense(
                        UUID.randomUUID(),
                        emissionDate.minusDays(3),
                        "gasto",
                        BigDecimal.valueOf(30),
                        BigDecimal.valueOf(6.30)
                )))
                .discounts(List.of(BigDecimal.TEN))
                .pdfPath("/tmp/invoice.pdf")
                .build();
    }
}
