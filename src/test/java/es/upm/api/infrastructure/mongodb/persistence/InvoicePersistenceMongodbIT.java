package es.upm.api.infrastructure.mongodb.persistence;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.Income;
import es.upm.api.domain.model.Invoice;
import es.upm.api.infrastructure.mongodb.entities.InvoiceEntity;
import es.upm.api.infrastructure.mongodb.repositories.InvoiceRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class InvoicePersistenceMongodbIT {

    @Autowired
    private InvoicePersistenceMongodb invoicePersistenceMongodb;

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
    void shouldPropagateExceptionWhenRepositoryFails() {
        RuntimeException exception = new RuntimeException("Mongo error");
        when(this.invoiceRepository.save(any(InvoiceEntity.class))).thenThrow(exception);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> this.invoicePersistenceMongodb.create(this.invoice));

        assertEquals("Mongo error", thrown.getMessage());
        verify(this.invoiceRepository).save(any(InvoiceEntity.class));
    }

    @Test
    void shouldFindAllInvoices() {
        when(this.invoiceRepository.findAll()).thenReturn(List.of(new InvoiceEntity(this.invoice)));

        List<Invoice> invoices = this.invoicePersistenceMongodb.findAll().toList();

        assertEquals(1, invoices.size());
        assertEquals(this.invoice, invoices.get(0));
        verify(this.invoiceRepository).findAll();
    }

    @Test
    void shouldFindInvoicesByEngagementId() {
        UUID engagementId = this.invoice.getEngagementId();
        when(this.invoiceRepository.findByEngagementId(engagementId)).thenReturn(List.of(new InvoiceEntity(this.invoice)));

        List<Invoice> invoices = this.invoicePersistenceMongodb.findByEngagementId(engagementId).toList();

        assertEquals(1, invoices.size());
        assertEquals(this.invoice, invoices.get(0));
        verify(this.invoiceRepository).findByEngagementId(engagementId);
    }

    @Test
    void shouldReturnEmptyWhenNoInvoicesFoundByEngagementId() {
        UUID engagementId = UUID.randomUUID();
        when(this.invoiceRepository.findByEngagementId(engagementId)).thenReturn(List.of());

        List<Invoice> invoices = this.invoicePersistenceMongodb.findByEngagementId(engagementId).toList();

        assertEquals(0, invoices.size());
        verify(this.invoiceRepository).findByEngagementId(engagementId);
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
