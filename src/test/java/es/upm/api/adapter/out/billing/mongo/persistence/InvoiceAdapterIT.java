package es.upm.api.adapter.out.billing.mongo.persistence;

import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceAdapter;
import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceEntity;
import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceRepository;
import es.upm.api.adapter.out.billing.mongo.invoice.LegalProcedureEntity;
import es.upm.api.domain.model.*;
import es.upm.api.domain.model.creation.LegalProcedure;
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
        assertEquals(this.invoice.getId().toString(), captor.getValue().getId());
        assertEquals(this.invoice.getConcept(), captor.getValue().getConcept());
        assertEquals(this.invoice.getClosed(), captor.getValue().getClosed());
        assertEquals(this.invoice.getEngagement().getId().toString(), captor.getValue().getEngagementId());
        assertEquals(this.invoice.getLegalProcedures(),
                captor.getValue().getLegalProcedures().stream().map(LegalProcedureEntity::toDomain).toList());
        assertEquals(this.invoice.getBaseAmount(), captor.getValue().getBaseAmount());
        assertEquals(this.invoice.getWithholdingRate(), captor.getValue().getWithholdingRate());
        assertEquals(this.invoice.getPriorPayments(),
                captor.getValue().getPriorPayments().stream().map(paymentEntity -> paymentEntity.toDomain()).toList());
        assertEquals(this.invoice.getExpenses(), captor.getValue().toDomain().getExpenses());
        assertEquals(this.invoice.getOriginalInvoice().getSeries(), captor.getValue().getOriginalInvoice().getSeries());
        assertEquals(this.invoice.getOriginalInvoice().getReason(), captor.getValue().getOriginalInvoice().getReason());
    }

    @Test
    void shouldReadInvoiceById() {
        when(this.invoiceRepository.findById(this.invoice.getId().toString()))
                .thenReturn(Optional.of(new InvoiceEntity(this.invoice)));

        Invoice read = this.invoiceAdapter.read(this.invoice.getId());

        assertEquals(this.invoice, read);
        verify(this.invoiceRepository).findById(this.invoice.getId().toString());
    }

    @Test
    void shouldReadInvoiceBySeriesAndNumber() {
        when(this.invoiceRepository.findBySeriesAndNumber("A", 1))
                .thenReturn(Optional.of(new InvoiceEntity(this.invoice)));

        Invoice read = this.invoiceAdapter.read("A", 1);

        assertEquals(this.invoice.getId(), read.getId());
        verify(this.invoiceRepository).findBySeriesAndNumber("A", 1);
    }

    @Test
    void shouldUpdateInvoice() {
        UUID id = this.invoice.getId();
        when(this.invoiceRepository.findById(id.toString())).thenReturn(Optional.of(new InvoiceEntity(this.invoice)));
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
        when(this.invoiceRepository.findById(id.toString())).thenReturn(Optional.of(entity));

        this.invoiceAdapter.delete(id);

        verify(this.invoiceRepository).delete(entity);
    }

    @Test
    void shouldThrowNotFoundWhenInvoiceMissing() {
        UUID id = this.invoice.getId();
        when(this.invoiceRepository.findById(id.toString())).thenReturn(Optional.empty());

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
    void shouldFindInvoicesByEngagementIdPrefix() {
        String engagementIdPrefix = this.engagementId.toString().substring(0, 4);
        InvoiceFindCriteria findCriteria = new InvoiceFindCriteria();
        findCriteria.setEngagementId(engagementIdPrefix);
        when(this.invoiceRepository.findByEngagementIdStartingWithOrderByEmissionDateDesc(engagementIdPrefix))
                .thenReturn(List.of(new InvoiceEntity(this.invoice)));

        List<Invoice> result = this.invoiceAdapter.find(findCriteria).toList();

        assertEquals(1, result.size());
        assertEquals(this.invoice.getId(), result.get(0).getId());
        verify(this.invoiceRepository).findByEngagementIdStartingWithOrderByEmissionDateDesc(engagementIdPrefix);
    }

    @Test
    void shouldFindInvoicesByEngagementIdPrefixAndFromDate() {
        LocalDate fromDate = LocalDate.of(2026, 3, 20);
        String engagementIdPrefix = this.engagementId.toString().substring(0, 4);
        InvoiceFindCriteria findCriteria = new InvoiceFindCriteria();
        findCriteria.setFromDate(fromDate);
        findCriteria.setEngagementId(engagementIdPrefix);
        when(this.invoiceRepository.findByEngagementIdStartingWithAndEmissionDateGreaterThanEqualOrderByEmissionDateDesc(
                engagementIdPrefix, fromDate))
                .thenReturn(List.of(new InvoiceEntity(this.invoice)));

        List<Invoice> result = this.invoiceAdapter.find(findCriteria).toList();

        assertEquals(1, result.size());
        assertEquals(this.invoice.getId(), result.get(0).getId());
        verify(this.invoiceRepository).findByEngagementIdStartingWithAndEmissionDateGreaterThanEqualOrderByEmissionDateDesc(
                engagementIdPrefix, fromDate);
    }

    @Test
    void shouldFindOnlyIssuedInvoices() {
        Invoice unissuedInvoice = this.buildInvoice(UUID.randomUUID(), this.engagementId, this.userId, this.paymentId,
                LocalDate.of(2026, 3, 21), BigDecimal.valueOf(120));
        unissuedInvoice.setEmissionDate(null);
        InvoiceFindCriteria findCriteria = new InvoiceFindCriteria();
        findCriteria.setIssued(true);
        when(this.invoiceRepository.findAllByOrderByEmissionDateDesc())
                .thenReturn(List.of(new InvoiceEntity(this.invoice), new InvoiceEntity(unissuedInvoice)));

        List<Invoice> result = this.invoiceAdapter.find(findCriteria).toList();

        assertEquals(1, result.size());
        assertEquals(this.invoice.getId(), result.getFirst().getId());
        verify(this.invoiceRepository).findAllByOrderByEmissionDateDesc();
    }

    @Test
    void shouldFindOnlyNotIssuedInvoices() {
        Invoice unissuedInvoice = this.buildInvoice(UUID.randomUUID(), this.engagementId, this.userId, this.paymentId,
                LocalDate.of(2026, 3, 21), BigDecimal.valueOf(120));
        unissuedInvoice.setEmissionDate(null);
        InvoiceFindCriteria findCriteria = new InvoiceFindCriteria();
        findCriteria.setIssued(false);
        when(this.invoiceRepository.findAllByOrderByEmissionDateDesc())
                .thenReturn(List.of(new InvoiceEntity(this.invoice), new InvoiceEntity(unissuedInvoice)));

        List<Invoice> result = this.invoiceAdapter.find(findCriteria).toList();

        assertEquals(1, result.size());
        assertEquals(unissuedInvoice.getId(), result.getFirst().getId());
        verify(this.invoiceRepository).findAllByOrderByEmissionDateDesc();
    }

    @Test
    void shouldFindIssuedInvoicesBetweenDates() {
        LocalDate fromDate = LocalDate.of(2026, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 3, 31);
        when(this.invoiceRepository.findIssuedBetweenOrderByNumberAsc(fromDate, toDate))
                .thenReturn(List.of(new InvoiceEntity(this.invoice)));

        List<Invoice> result = this.invoiceAdapter.findIssuedBetween(fromDate, toDate).toList();

        assertEquals(1, result.size());
        assertEquals(this.invoice.getId(), result.getFirst().getId());
        verify(this.invoiceRepository).findIssuedBetweenOrderByNumberAsc(fromDate, toDate);
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
                .withholdingRate(new BigDecimal("15"))
                .engagement(EngagementSnapshot.builder().id(engagementId).build())
                .legalProcedures(List.of(LegalProcedure.builder()
                        .title("Procedimiento")
                        .budget(baseAmount)
                        .legalTasks(List.of("Tarea 1"))
                        .build()))
                .payments(List.of(Payment.builder()
                        .id(paymentId)
                        .date(emissionDate.minusDays(2))
                        .amount(baseAmount.add(BigDecimal.TEN))
                        .method(PaymentMethod.TRANSFER)
                        .user(UserSnapshot.builder().id(userId).build())
                        .build()))
                .priorPayments(List.of(Payment.builder()
                        .id(UUID.randomUUID())
                        .date(emissionDate.minusDays(5))
                        .amount(BigDecimal.TEN)
                        .method(PaymentMethod.CASH)
                        .user(UserSnapshot.builder().id(userId).build())
                        .build()))
                .expenses(List.of(Expense.builder()
                        .id(UUID.randomUUID())
                        .issueDate(emissionDate.minusDays(3))
                        .description("gasto")
                        .baseAmount(BigDecimal.valueOf(30))
                        .vatRate(21)
                        .depreciationRate(100)
                        .build()))
                .originalInvoice(OriginalInvoice.builder()
                        .series("2025")
                        .number(29)
                        .emissionDate(emissionDate.minusDays(10))
                        .reason("Error en datos fiscales")
                        .build())
                .discounts(List.of(BigDecimal.TEN))
                .pdfPath("/tmp/invoice.pdf")
                .build();
    }
}
