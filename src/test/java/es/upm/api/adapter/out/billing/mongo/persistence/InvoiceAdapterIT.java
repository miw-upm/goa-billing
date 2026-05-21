package es.upm.api.adapter.out.billing.mongo.persistence;

import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceAdapter;
import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.Income;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.criteria.InvoiceFindCriteria;
import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceEntity;
import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class InvoiceAdapterIT {

    @Autowired
    private InvoiceAdapter invoicePersistenceMongodb;

    @MockitoBean
    private InvoiceRepository invoiceRepository;

    private Invoice invoice;

    @BeforeEach
    void setUp() {
        UUID engagementId = UUID.randomUUID();
        this.invoice = Invoice.builder()
                .id(UUID.randomUUID())
                .engagementId(engagementId)
                .date(LocalDate.of(2026, 3, 21))
                .expenses(List.of(Expense.builder()
                        .id(UUID.randomUUID())
                        .engagementId(engagementId)
                        .amount(BigDecimal.valueOf(25))
                        .date(LocalDate.of(2026, 3, 20))
                        .description("Taxi")
                        .build()))
                .incomes(List.of(Income.builder()
                        .id(UUID.randomUUID())
                        .engagementId(engagementId)
                        .userId(UUID.randomUUID())
                        .amount(BigDecimal.valueOf(250))
                        .date(LocalDate.of(2026, 3, 20))
                        .build()))
                .build();
    }

    @Test
    void shouldCreateInvoice() {
        when(this.invoiceRepository.save(any(InvoiceEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        this.invoicePersistenceMongodb.create(this.invoice);

        ArgumentCaptor<InvoiceEntity> invoiceEntityCaptor = ArgumentCaptor.forClass(InvoiceEntity.class);
        verify(this.invoiceRepository).save(invoiceEntityCaptor.capture());

        InvoiceEntity persistedInvoiceEntity = invoiceEntityCaptor.getValue();
        assertEquals(this.invoice.getId(), persistedInvoiceEntity.getId());
        assertEquals(this.invoice.getEngagementId(), persistedInvoiceEntity.getEngagementId());
        assertEquals(this.invoice.getDate(), persistedInvoiceEntity.getDate());
        assertEquals(this.invoice.getExpenses(), persistedInvoiceEntity.getExpenses());
        assertEquals(this.invoice.getIncomes(), persistedInvoiceEntity.getIncomes());
    }

    @Test
    void shouldReadInvoiceById() {
        when(this.invoiceRepository.findById(this.invoice.getId())).thenReturn(Optional.of(new InvoiceEntity(this.invoice)));

        Invoice readInvoice = this.invoicePersistenceMongodb.readById(this.invoice.getId());

        assertEquals(this.invoice, readInvoice);
        verify(this.invoiceRepository).findById(this.invoice.getId());
    }

    @Test
    void shouldUpdateInvoice() {
        Invoice updatedInvoice = Invoice.builder()
                .id(this.invoice.getId())
                .engagementId(UUID.randomUUID())
                .date(LocalDate.of(2026, 3, 25))
                .expenses(List.of())
                .incomes(this.invoice.getIncomes())
                .build();
        when(this.invoiceRepository.findById(this.invoice.getId())).thenReturn(Optional.of(new InvoiceEntity(this.invoice)));
        when(this.invoiceRepository.save(any(InvoiceEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Invoice persistedInvoice = this.invoicePersistenceMongodb.update(this.invoice.getId(), updatedInvoice);

        assertEquals(updatedInvoice, persistedInvoice);
        verify(this.invoiceRepository).findById(this.invoice.getId());
        verify(this.invoiceRepository).save(any(InvoiceEntity.class));
    }

    @Test
    void shouldFailReadInvoiceByIdWhenInvoiceDoesNotExist() {
        UUID invoiceId = UUID.randomUUID();
        when(this.invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        NotFoundException thrown = assertThrows(NotFoundException.class,
                () -> this.invoicePersistenceMongodb.readById(invoiceId));

        assertEquals("Not Found Exception. Invoice id: " + invoiceId, thrown.getMessage());
        verify(this.invoiceRepository).findById(invoiceId);
    }

    @Test
    void shouldPropagateExceptionWhenRepositoryFails() {
        RuntimeException exception = new RuntimeException("Mongo error");
        when(this.invoiceRepository.save(any(InvoiceEntity.class))).thenThrow(exception);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> this.invoicePersistenceMongodb.create(this.invoice));

        assertEquals("Mongo error", thrown.getMessage());
        verify(this.invoiceRepository).save(any(InvoiceEntity.class));
    }

    @Test
    void shouldFailUpdateInvoiceWhenInvoiceDoesNotExist() {
        UUID invoiceId = UUID.randomUUID();
        when(this.invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        NotFoundException thrown = assertThrows(NotFoundException.class,
                () -> this.invoicePersistenceMongodb.update(invoiceId, this.invoice));

        assertEquals("Not Found Exception. Invoice id: " + invoiceId, thrown.getMessage());
        verify(this.invoiceRepository).findById(invoiceId);
    }

    @Test
    void shouldFindAllInvoices() {
        InvoiceFindCriteria criteria = new InvoiceFindCriteria();
        when(this.invoiceRepository.findAll(InvoiceAdapter.DATE)).thenReturn(List.of(new InvoiceEntity(this.invoice)));

        List<Invoice> invoices = this.invoicePersistenceMongodb.findAll(criteria).toList();

        assertEquals(1, invoices.size());
        assertEquals(this.invoice, invoices.get(0));
        verify(this.invoiceRepository).findAll(InvoiceAdapter.DATE);
    }

    @Test
    void shouldFindInvoicesByEngagementId() {
        UUID engagementId = this.invoice.getEngagementId();
        InvoiceFindCriteria criteria = new InvoiceFindCriteria(engagementId, null);
        when(this.invoiceRepository.findByEngagementId(engagementId, InvoiceAdapter.DATE))
                .thenReturn(List.of(new InvoiceEntity(this.invoice)));

        List<Invoice> invoices = this.invoicePersistenceMongodb.findAll(criteria).toList();

        assertEquals(1, invoices.size());
        assertEquals(this.invoice, invoices.get(0));
        verify(this.invoiceRepository).findByEngagementId(engagementId, InvoiceAdapter.DATE);
    }

    @Test
    void shouldReturnEmptyWhenNoInvoicesFoundByEngagementId() {
        UUID engagementId = UUID.randomUUID();
        InvoiceFindCriteria criteria = new InvoiceFindCriteria(engagementId, null);
        when(this.invoiceRepository.findByEngagementId(engagementId, InvoiceAdapter.DATE)).thenReturn(List.of());

        List<Invoice> invoices = this.invoicePersistenceMongodb.findAll(criteria).toList();

        assertEquals(0, invoices.size());
        verify(this.invoiceRepository).findByEngagementId(engagementId, InvoiceAdapter.DATE);
    }

    @Test
    void shouldFindInvoicesByDate() {
        LocalDate date = this.invoice.getDate();
        InvoiceFindCriteria criteria = new InvoiceFindCriteria(null, date);
        when(this.invoiceRepository.findByDate(date, InvoiceAdapter.DATE))
                .thenReturn(List.of(new InvoiceEntity(this.invoice)));

        List<Invoice> invoices = this.invoicePersistenceMongodb.findAll(criteria).toList();

        assertEquals(1, invoices.size());
        assertEquals(this.invoice, invoices.get(0));
        verify(this.invoiceRepository).findByDate(date, InvoiceAdapter.DATE);
    }

    @Test
    void shouldFindInvoicesByEngagementIdAndDate() {
        UUID engagementId = this.invoice.getEngagementId();
        LocalDate date = this.invoice.getDate();
        InvoiceFindCriteria criteria = new InvoiceFindCriteria(engagementId, date);
        when(this.invoiceRepository.findByEngagementIdAndDate(engagementId, date, InvoiceAdapter.DATE))
                .thenReturn(List.of(new InvoiceEntity(this.invoice)));

        List<Invoice> invoices = this.invoicePersistenceMongodb.findAll(criteria).toList();

        assertEquals(1, invoices.size());
        assertEquals(this.invoice, invoices.get(0));
        verify(this.invoiceRepository).findByEngagementIdAndDate(engagementId, date, InvoiceAdapter.DATE);
    }

    @Test
    void shouldFindInvoiceByExpenseId() {
        UUID expenseId = this.invoice.getExpenses().get(0).getId();
        when(this.invoiceRepository.findByExpensesId(expenseId)).thenReturn(new InvoiceEntity(this.invoice));

        Invoice foundInvoice = this.invoicePersistenceMongodb.findByExpenseId(expenseId);

        assertEquals(this.invoice, foundInvoice);
        verify(this.invoiceRepository).findByExpensesId(expenseId);
    }

    @Test
    void shouldReturnNullWhenExpenseIsNotAssigned() {
        UUID expenseId = this.invoice.getExpenses().get(0).getId();
        when(this.invoiceRepository.findByExpensesId(expenseId)).thenReturn(null);

        Invoice foundInvoice = this.invoicePersistenceMongodb.findByExpenseId(expenseId);

        assertEquals(null, foundInvoice);
        verify(this.invoiceRepository).findByExpensesId(expenseId);
    }

    @Test
    void shouldFindInvoiceByIncomeId() {
        UUID incomeId = this.invoice.getIncomes().get(0).getId();
        when(this.invoiceRepository.findByIncomesId(incomeId)).thenReturn(new InvoiceEntity(this.invoice));

        Invoice foundInvoice = this.invoicePersistenceMongodb.findByIncomeId(incomeId);

        assertEquals(this.invoice, foundInvoice);
        verify(this.invoiceRepository).findByIncomesId(incomeId);
    }

    @Test
    void shouldReturnNullWhenIncomeIsNotAssigned() {
        UUID incomeId = this.invoice.getIncomes().get(0).getId();
        when(this.invoiceRepository.findByIncomesId(incomeId)).thenReturn(null);

        Invoice foundInvoice = this.invoicePersistenceMongodb.findByIncomeId(incomeId);

        assertEquals(null, foundInvoice);
        verify(this.invoiceRepository).findByIncomesId(incomeId);
    }
}
