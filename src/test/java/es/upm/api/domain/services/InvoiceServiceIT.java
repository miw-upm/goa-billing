package es.upm.api.domain.services;

import es.upm.api.domain.exceptions.BadRequestException;
import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.Income;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.persistence.ExpensePersistence;
import es.upm.api.domain.persistence.IncomePersistence;
import es.upm.api.domain.persistence.InvoicePersistence;
import es.upm.api.domain.webclients.EngagementWebClient;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
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
    private InvoicePersistence invoicePersistence;

    @MockitoBean
    private ExpensePersistence expensePersistence;

    @MockitoBean
    private IncomePersistence incomePersistence;

    @MockitoBean
    private EngagementWebClient engagementWebClient;

    private Invoice invoice;
    private Expense expense;
    private Income income;

    @BeforeEach
    void setUp() {
        UUID engagementId = UUID.randomUUID();
        this.expense = Expense.builder()
                .id(UUID.randomUUID())
                .engagementId(engagementId)
                .amount(BigDecimal.valueOf(25))
                .date(LocalDate.of(2026, 3, 20))
                .description("Taxi")
                .build();
        this.income = Income.builder()
                .id(UUID.randomUUID())
                .engagementId(engagementId)
                .userId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(250))
                .date(LocalDate.of(2026, 3, 20))
                .build();
        this.invoice = Invoice.builder()
                .engagementId(engagementId)
                .date(LocalDate.of(2026, 3, 21))
                .expenses(List.of(Expense.builder().id(this.expense.getId()).build()))
                .incomes(List.of(Income.builder().id(this.income.getId()).build()))
                .build();
    }

    @Test
    void shouldCreateInvoice() {
        when(this.engagementWebClient.readById(this.invoice.getEngagementId())).thenReturn(new Object());
        when(this.expensePersistence.readById(this.expense.getId())).thenReturn(this.expense);
        when(this.incomePersistence.readById(this.income.getId())).thenReturn(this.income);

        Invoice createdInvoice = this.invoiceService.create(this.invoice);

        assertNotNull(createdInvoice);
        assertNotNull(createdInvoice.getId());
        assertEquals(this.invoice.getEngagementId(), createdInvoice.getEngagementId());
        assertEquals(this.invoice.getDate(), createdInvoice.getDate());
        assertEquals(this.invoice.getExpenses(), createdInvoice.getExpenses());
        assertEquals(this.invoice.getIncomes(), createdInvoice.getIncomes());

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(this.invoicePersistence).create(invoiceCaptor.capture());
        verify(this.engagementWebClient).readById(this.invoice.getEngagementId());
        verify(this.expensePersistence).readById(this.expense.getId());
        verify(this.incomePersistence).readById(this.income.getId());

        Invoice persistedInvoice = invoiceCaptor.getValue();
        assertNotNull(persistedInvoice.getId());
        assertEquals(createdInvoice.getId(), persistedInvoice.getId());
        assertEquals(this.invoice.getEngagementId(), persistedInvoice.getEngagementId());
        assertEquals(this.invoice.getDate(), persistedInvoice.getDate());
        assertEquals(this.invoice.getExpenses(), persistedInvoice.getExpenses());
        assertEquals(this.invoice.getIncomes(), persistedInvoice.getIncomes());
    }

    @Test
    void shouldCreateInvoiceWithOnlyExpenses() {
        this.invoice.setIncomes(List.of());
        when(this.engagementWebClient.readById(this.invoice.getEngagementId())).thenReturn(new Object());
        when(this.expensePersistence.readById(this.expense.getId())).thenReturn(this.expense);

        Invoice createdInvoice = this.invoiceService.create(this.invoice);

        assertNotNull(createdInvoice.getId());
        assertEquals(1, createdInvoice.getExpenses().size());
        assertEquals(0, createdInvoice.getIncomes().size());
        verify(this.engagementWebClient).readById(this.invoice.getEngagementId());
        verify(this.expensePersistence).readById(this.expense.getId());
        verify(this.incomePersistence, never()).readById(any());
        verify(this.invoicePersistence).create(any());
    }

    @Test
    void shouldCreateInvoiceWithOnlyIncomes() {
        this.invoice.setExpenses(List.of());
        when(this.engagementWebClient.readById(this.invoice.getEngagementId())).thenReturn(new Object());
        when(this.incomePersistence.readById(this.income.getId())).thenReturn(this.income);

        Invoice createdInvoice = this.invoiceService.create(this.invoice);

        assertNotNull(createdInvoice.getId());
        assertEquals(0, createdInvoice.getExpenses().size());
        assertEquals(1, createdInvoice.getIncomes().size());
        verify(this.engagementWebClient).readById(this.invoice.getEngagementId());
        verify(this.expensePersistence, never()).readById(any());
        verify(this.incomePersistence).readById(this.income.getId());
        verify(this.invoicePersistence).create(any());
    }

    @Test
    void shouldNotPersistInvoiceWhenItHasNoExpensesAndNoIncomes() {
        this.invoice.setExpenses(List.of());
        this.invoice.setIncomes(List.of());

        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> this.invoiceService.create(this.invoice));

        assertEquals("Bad Request Exception. Invoice must contain at least one expense or one income", thrown.getMessage());
        verifyNoInteractions(this.engagementWebClient);
        verifyNoInteractions(this.expensePersistence);
        verifyNoInteractions(this.incomePersistence);
        verify(this.invoicePersistence, never()).create(any());
    }

    @Test
    void shouldNotPersistInvoiceWhenDateIsFuture() {
        this.invoice.setDate(LocalDate.now().plusDays(1));

        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> this.invoiceService.create(this.invoice));

        assertEquals("Bad Request Exception. Invoice date cannot be in the future", thrown.getMessage());
        verifyNoInteractions(this.engagementWebClient);
        verifyNoInteractions(this.expensePersistence);
        verifyNoInteractions(this.incomePersistence);
        verify(this.invoicePersistence, never()).create(any());
    }

    @Test
    void shouldNotPersistInvoiceWhenEngagementDoesNotExist() {
        RuntimeException exception = new RuntimeException("Engagement not found");
        when(this.engagementWebClient.readById(this.invoice.getEngagementId())).thenThrow(exception);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> this.invoiceService.create(this.invoice));

        assertEquals("Engagement not found", thrown.getMessage());
        verify(this.engagementWebClient).readById(this.invoice.getEngagementId());
        verify(this.expensePersistence, never()).readById(any());
        verify(this.incomePersistence, never()).readById(any());
        verify(this.invoicePersistence, never()).create(any());
    }

    @Test
    void shouldNotPersistInvoiceWhenExpenseDoesNotBelongToEngagement() {
        when(this.engagementWebClient.readById(this.invoice.getEngagementId())).thenReturn(new Object());
        when(this.expensePersistence.readById(this.expense.getId())).thenReturn(
                Expense.builder()
                        .id(this.expense.getId())
                        .engagementId(UUID.randomUUID())
                        .amount(this.expense.getAmount())
                        .date(this.expense.getDate())
                        .description(this.expense.getDescription())
                        .build()
        );

        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> this.invoiceService.create(this.invoice));

        assertEquals("Bad Request Exception. Expense does not belong to the invoice engagement", thrown.getMessage());
        verify(this.engagementWebClient).readById(this.invoice.getEngagementId());
        verify(this.expensePersistence).readById(this.expense.getId());
        verify(this.incomePersistence, never()).readById(any());
        verify(this.invoicePersistence, never()).create(any());
    }

    @Test
    void shouldNotPersistInvoiceWhenIncomeDoesNotBelongToEngagement() {
        when(this.engagementWebClient.readById(this.invoice.getEngagementId())).thenReturn(new Object());
        when(this.expensePersistence.readById(this.expense.getId())).thenReturn(this.expense);
        when(this.incomePersistence.readById(this.income.getId())).thenReturn(
                Income.builder()
                        .id(this.income.getId())
                        .engagementId(UUID.randomUUID())
                        .userId(this.income.getUserId())
                        .amount(this.income.getAmount())
                        .date(this.income.getDate())
                        .build()
        );

        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> this.invoiceService.create(this.invoice));

        assertEquals("Bad Request Exception. Income does not belong to the invoice engagement", thrown.getMessage());
        verify(this.engagementWebClient).readById(this.invoice.getEngagementId());
        verify(this.expensePersistence).readById(this.expense.getId());
        verify(this.incomePersistence).readById(this.income.getId());
        verify(this.invoicePersistence, never()).create(any());
    }
}
