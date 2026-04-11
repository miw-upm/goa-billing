package es.upm.api.domain.services;

import es.upm.api.domain.exceptions.BadRequestException;
import es.upm.api.domain.exceptions.NotFoundException;
import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.Income;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.InvoiceFindCriteria;
import es.upm.api.domain.model.InvoiceBreakdown;
import es.upm.api.domain.model.BreakdownItem;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
    void shouldGetInvoiceBreakdown() {
        UUID invoiceId = UUID.randomUUID();
        this.invoice.setId(invoiceId);
        this.invoice.setExpenses(List.of(this.expense));
        this.invoice.setIncomes(List.of(this.income));

        when(this.invoicePersistence.readById(invoiceId)).thenReturn(this.invoice);

        InvoiceBreakdown breakdown = this.invoiceService.getInvoiceBreakdown(invoiceId);

        assertNotNull(breakdown);

        assertEquals(new BigDecimal("185.95"), breakdown.getTaxableBase().setScale(2));
        assertEquals(new BigDecimal("39.05"), breakdown.getVatAmount().setScale(2));
        assertEquals(new BigDecimal("225.00"), breakdown.getTotalAmount().setScale(2));

        assertEquals(1, breakdown.getIncomes().size());
        assertEquals(this.income.getId(), breakdown.getIncomes().get(0).getId());
        assertEquals(this.income.getAmount(), breakdown.getIncomes().get(0).getAmountWithVat());
        assertEquals(new BigDecimal("206.61"), breakdown.getIncomes().get(0).getTaxableBase().setScale(2));
        assertEquals(new BigDecimal("43.39"), breakdown.getIncomes().get(0).getVatAmount().setScale(2));

        assertEquals(1, breakdown.getExpenses().size());
        assertEquals(this.expense.getId(), breakdown.getExpenses().get(0).getId());
        assertEquals(this.expense.getAmount(), breakdown.getExpenses().get(0).getAmountWithVat());
        assertEquals(new BigDecimal("20.66"), breakdown.getExpenses().get(0).getTaxableBase().setScale(2));
        assertEquals(new BigDecimal("4.34"), breakdown.getExpenses().get(0).getVatAmount().setScale(2));

        verify(this.invoicePersistence).readById(invoiceId);
    }

    @Test
    void whenGetInvoiceBreakdownOfNull_shouldThrowBadRequestException() {
        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> this.invoiceService.getInvoiceBreakdown(null));

        assertEquals("Bad Request Exception. Invoice not found", thrown.getMessage());
        verify(this.invoicePersistence).readById(null);
    }

    @Test
    void testCalculateIncomeBreakdown() {
        BreakdownItem result = invoiceService.calculateIncomeBreakdown(income);
        assertEquals(income.getId(), result.getId());
        assertEquals(new BigDecimal("250.00"), result.getAmountWithVat().setScale(2));
        assertEquals(new BigDecimal("206.61"), result.getTaxableBase());
        assertEquals(new BigDecimal("43.39"), result.getVatAmount());
    }

    @Test
    void testCalculateExpenseBreakdown() {
        BreakdownItem result = invoiceService.calculateExpenseBreakdown(expense);
        assertEquals(expense.getId(), result.getId());
        assertEquals(new BigDecimal("25.00"), result.getAmountWithVat().setScale(2));
        assertEquals(new BigDecimal("20.66"), result.getTaxableBase());
        assertEquals(new BigDecimal("4.34"), result.getVatAmount());
    }

    @Test
    void shouldCreateInvoice() {
        when(this.engagementWebClient.readById(this.invoice.getEngagementId())).thenReturn(new Object());
        when(this.expensePersistence.readById(this.expense.getId())).thenReturn(this.expense);
        when(this.incomePersistence.readById(this.income.getId())).thenReturn(this.income);
        when(this.invoicePersistence.findByExpenseId(this.expense.getId())).thenReturn(null);
        when(this.invoicePersistence.findByIncomeId(this.income.getId())).thenReturn(null);

        Invoice createdInvoice = this.invoiceService.create(this.invoice);

        assertNotNull(createdInvoice);
        assertNotNull(createdInvoice.getId());
        assertEquals(this.invoice.getEngagementId(), createdInvoice.getEngagementId());
        assertEquals(this.invoice.getDate(), createdInvoice.getDate());
        assertEquals(List.of(this.expense), createdInvoice.getExpenses());
        assertEquals(List.of(this.income), createdInvoice.getIncomes());

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
        assertEquals(List.of(this.expense), persistedInvoice.getExpenses());
        assertEquals(List.of(this.income), persistedInvoice.getIncomes());
    }

    @Test
    void shouldCreateInvoiceWithOnlyExpenses() {
        this.invoice.setIncomes(List.of());
        when(this.engagementWebClient.readById(this.invoice.getEngagementId())).thenReturn(new Object());
        when(this.expensePersistence.readById(this.expense.getId())).thenReturn(this.expense);
        when(this.invoicePersistence.findByExpenseId(this.expense.getId())).thenReturn(null);

        Invoice createdInvoice = this.invoiceService.create(this.invoice);

        assertNotNull(createdInvoice.getId());
        assertEquals(List.of(this.expense), createdInvoice.getExpenses());
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
        when(this.invoicePersistence.findByIncomeId(this.income.getId())).thenReturn(null);

        Invoice createdInvoice = this.invoiceService.create(this.invoice);

        assertNotNull(createdInvoice.getId());
        assertEquals(0, createdInvoice.getExpenses().size());
        assertEquals(List.of(this.income), createdInvoice.getIncomes());
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
    void shouldNotPersistInvoiceWhenExpenseAlreadyAssignedToAnotherInvoice() {
        when(this.engagementWebClient.readById(this.invoice.getEngagementId())).thenReturn(new Object());
        when(this.expensePersistence.readById(this.expense.getId())).thenReturn(this.expense);
        when(this.invoicePersistence.findByExpenseId(this.expense.getId())).thenReturn(Invoice.builder().id(UUID.randomUUID()).build());

        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> this.invoiceService.create(this.invoice));

        assertEquals("Bad Request Exception. Expense is already assigned to another invoice", thrown.getMessage());
        verify(this.engagementWebClient).readById(this.invoice.getEngagementId());
        verify(this.expensePersistence).readById(this.expense.getId());
        verify(this.invoicePersistence).findByExpenseId(this.expense.getId());
        verify(this.incomePersistence, never()).readById(any());
        verify(this.invoicePersistence, never()).create(any());
    }

    @Test
    void shouldNotPersistInvoiceWhenIncomeDoesNotBelongToEngagement() {
        when(this.engagementWebClient.readById(this.invoice.getEngagementId())).thenReturn(new Object());
        when(this.expensePersistence.readById(this.expense.getId())).thenReturn(this.expense);
        when(this.invoicePersistence.findByExpenseId(this.expense.getId())).thenReturn(null);
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

    @Test
    void shouldNotPersistInvoiceWhenIncomeAlreadyAssignedToAnotherInvoice() {
        when(this.engagementWebClient.readById(this.invoice.getEngagementId())).thenReturn(new Object());
        when(this.expensePersistence.readById(this.expense.getId())).thenReturn(this.expense);
        when(this.invoicePersistence.findByExpenseId(this.expense.getId())).thenReturn(null);
        when(this.incomePersistence.readById(this.income.getId())).thenReturn(this.income);
        when(this.invoicePersistence.findByIncomeId(this.income.getId())).thenReturn(Invoice.builder().id(UUID.randomUUID()).build());

        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> this.invoiceService.create(this.invoice));

        assertEquals("Bad Request Exception. Income is already assigned to another invoice", thrown.getMessage());
        verify(this.engagementWebClient).readById(this.invoice.getEngagementId());
        verify(this.expensePersistence).readById(this.expense.getId());
        verify(this.invoicePersistence).findByExpenseId(this.expense.getId());
        verify(this.incomePersistence).readById(this.income.getId());
        verify(this.invoicePersistence).findByIncomeId(this.income.getId());
        verify(this.invoicePersistence, never()).create(any());
    }

    @Test
    void shouldUpdateInvoice() {
        UUID invoiceId = UUID.randomUUID();
        this.invoice.setId(invoiceId);
        when(this.invoicePersistence.readById(invoiceId)).thenReturn(this.invoice);
        when(this.engagementWebClient.readById(this.invoice.getEngagementId())).thenReturn(new Object());
        when(this.expensePersistence.readById(this.expense.getId())).thenReturn(this.expense);
        when(this.incomePersistence.readById(this.income.getId())).thenReturn(this.income);
        when(this.invoicePersistence.findByExpenseId(this.expense.getId())).thenReturn(Invoice.builder().id(invoiceId).build());
        when(this.invoicePersistence.findByIncomeId(this.income.getId())).thenReturn(Invoice.builder().id(invoiceId).build());
        when(this.invoicePersistence.update(invoiceId, this.invoice)).thenReturn(this.invoice);

        Invoice updatedInvoice = this.invoiceService.update(invoiceId, this.invoice);

        assertEquals(invoiceId, updatedInvoice.getId());
        assertEquals(this.invoice.getEngagementId(), updatedInvoice.getEngagementId());
        assertEquals(this.invoice.getDate(), updatedInvoice.getDate());
        assertEquals(List.of(this.expense), updatedInvoice.getExpenses());
        assertEquals(List.of(this.income), updatedInvoice.getIncomes());
        verify(this.invoicePersistence).readById(invoiceId);
        verify(this.engagementWebClient).readById(this.invoice.getEngagementId());
        verify(this.expensePersistence).readById(this.expense.getId());
        verify(this.incomePersistence).readById(this.income.getId());
        verify(this.invoicePersistence).update(invoiceId, this.invoice);
    }

    @Test
    void shouldNotUpdateInvoiceWhenDateIsFuture() {
        UUID invoiceId = UUID.randomUUID();
        this.invoice.setDate(LocalDate.now().plusDays(1));
        when(this.invoicePersistence.readById(invoiceId)).thenReturn(Invoice.builder().id(invoiceId).build());

        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> this.invoiceService.update(invoiceId, this.invoice));

        assertEquals("Bad Request Exception. Invoice date cannot be in the future", thrown.getMessage());
        verify(this.invoicePersistence).readById(invoiceId);
        verifyNoInteractions(this.engagementWebClient);
        verify(this.invoicePersistence, never()).update(any(), any());
    }

    @Test
    void shouldNotUpdateInvoiceWhenItHasNoExpensesAndNoIncomes() {
        UUID invoiceId = UUID.randomUUID();
        this.invoice.setExpenses(List.of());
        this.invoice.setIncomes(List.of());
        when(this.invoicePersistence.readById(invoiceId)).thenReturn(Invoice.builder().id(invoiceId).build());

        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> this.invoiceService.update(invoiceId, this.invoice));

        assertEquals("Bad Request Exception. Invoice must contain at least one expense or one income", thrown.getMessage());
        verify(this.invoicePersistence).readById(invoiceId);
        verifyNoInteractions(this.engagementWebClient);
        verifyNoInteractions(this.expensePersistence);
        verifyNoInteractions(this.incomePersistence);
        verify(this.invoicePersistence, never()).update(any(), any());
    }

    @Test
    void shouldNotUpdateInvoiceWhenItDoesNotExist() {
        UUID invoiceId = UUID.randomUUID();
        when(this.invoicePersistence.readById(invoiceId))
                .thenThrow(new NotFoundException("Invoice id: " + invoiceId));

        NotFoundException thrown = assertThrows(NotFoundException.class,
                () -> this.invoiceService.update(invoiceId, this.invoice));

        assertEquals("Not Found Exception. Invoice id: " + invoiceId, thrown.getMessage());
        verify(this.invoicePersistence).readById(invoiceId);
        verifyNoInteractions(this.engagementWebClient);
        verify(this.invoicePersistence, never()).update(any(), any());
    }

    @Test
    void shouldNotUpdateInvoiceWhenEngagementDoesNotExist() {
        UUID invoiceId = UUID.randomUUID();
        this.invoice.setId(invoiceId);
        RuntimeException exception = new RuntimeException("Engagement not found");
        when(this.invoicePersistence.readById(invoiceId)).thenReturn(this.invoice);
        when(this.engagementWebClient.readById(this.invoice.getEngagementId())).thenThrow(exception);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> this.invoiceService.update(invoiceId, this.invoice));

        assertEquals("Engagement not found", thrown.getMessage());
        verify(this.invoicePersistence).readById(invoiceId);
        verify(this.engagementWebClient).readById(this.invoice.getEngagementId());
        verify(this.expensePersistence, never()).readById(any());
        verify(this.incomePersistence, never()).readById(any());
        verify(this.invoicePersistence, never()).update(any(), any());
    }

    @Test
    void shouldNotUpdateInvoiceWhenExpenseDoesNotBelongToEngagement() {
        UUID invoiceId = UUID.randomUUID();
        this.invoice.setId(invoiceId);
        when(this.invoicePersistence.readById(invoiceId)).thenReturn(this.invoice);
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
                () -> this.invoiceService.update(invoiceId, this.invoice));

        assertEquals("Bad Request Exception. Expense does not belong to the invoice engagement", thrown.getMessage());
        verify(this.invoicePersistence).readById(invoiceId);
        verify(this.engagementWebClient).readById(this.invoice.getEngagementId());
        verify(this.expensePersistence).readById(this.expense.getId());
        verify(this.incomePersistence, never()).readById(any());
        verify(this.invoicePersistence, never()).update(any(), any());
    }

    @Test
    void shouldNotUpdateInvoiceWhenIncomeDoesNotBelongToEngagement() {
        UUID invoiceId = UUID.randomUUID();
        this.invoice.setId(invoiceId);
        when(this.invoicePersistence.readById(invoiceId)).thenReturn(this.invoice);
        when(this.engagementWebClient.readById(this.invoice.getEngagementId())).thenReturn(new Object());
        when(this.expensePersistence.readById(this.expense.getId())).thenReturn(this.expense);
        when(this.invoicePersistence.findByExpenseId(this.expense.getId())).thenReturn(Invoice.builder().id(invoiceId).build());
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
                () -> this.invoiceService.update(invoiceId, this.invoice));

        assertEquals("Bad Request Exception. Income does not belong to the invoice engagement", thrown.getMessage());
        verify(this.invoicePersistence).readById(invoiceId);
        verify(this.engagementWebClient).readById(this.invoice.getEngagementId());
        verify(this.expensePersistence).readById(this.expense.getId());
        verify(this.incomePersistence).readById(this.income.getId());
        verify(this.invoicePersistence, never()).update(any(), any());
    }

    @Test
    void shouldNotUpdateInvoiceWhenExpenseAlreadyAssignedToAnotherInvoice() {
        UUID invoiceId = UUID.randomUUID();
        this.invoice.setId(invoiceId);
        when(this.invoicePersistence.readById(invoiceId)).thenReturn(this.invoice);
        when(this.engagementWebClient.readById(this.invoice.getEngagementId())).thenReturn(new Object());
        when(this.expensePersistence.readById(this.expense.getId())).thenReturn(this.expense);
        when(this.invoicePersistence.findByExpenseId(this.expense.getId())).thenReturn(Invoice.builder().id(UUID.randomUUID()).build());

        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> this.invoiceService.update(invoiceId, this.invoice));

        assertEquals("Bad Request Exception. Expense is already assigned to another invoice", thrown.getMessage());
        verify(this.invoicePersistence).readById(invoiceId);
        verify(this.engagementWebClient).readById(this.invoice.getEngagementId());
        verify(this.expensePersistence).readById(this.expense.getId());
        verify(this.invoicePersistence).findByExpenseId(this.expense.getId());
        verify(this.incomePersistence, never()).readById(any());
        verify(this.invoicePersistence, never()).update(any(), any());
    }

    @Test
    void shouldNotUpdateInvoiceWhenIncomeAlreadyAssignedToAnotherInvoice() {
        UUID invoiceId = UUID.randomUUID();
        this.invoice.setId(invoiceId);
        when(this.invoicePersistence.readById(invoiceId)).thenReturn(this.invoice);
        when(this.engagementWebClient.readById(this.invoice.getEngagementId())).thenReturn(new Object());
        when(this.expensePersistence.readById(this.expense.getId())).thenReturn(this.expense);
        when(this.invoicePersistence.findByExpenseId(this.expense.getId())).thenReturn(Invoice.builder().id(invoiceId).build());
        when(this.incomePersistence.readById(this.income.getId())).thenReturn(this.income);
        when(this.invoicePersistence.findByIncomeId(this.income.getId())).thenReturn(Invoice.builder().id(UUID.randomUUID()).build());

        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> this.invoiceService.update(invoiceId, this.invoice));

        assertEquals("Bad Request Exception. Income is already assigned to another invoice", thrown.getMessage());
        verify(this.invoicePersistence).readById(invoiceId);
        verify(this.engagementWebClient).readById(this.invoice.getEngagementId());
        verify(this.expensePersistence).readById(this.expense.getId());
        verify(this.incomePersistence).readById(this.income.getId());
        verify(this.invoicePersistence).findByIncomeId(this.income.getId());
        verify(this.invoicePersistence, never()).update(any(), any());
    }

    @Test
    void shouldFindAllInvoices() {
        Invoice invoiceA = Invoice.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .date(LocalDate.of(2026, 3, 20))
                .expenses(List.of())
                .incomes(List.of(this.income))
                .build();
        Invoice invoiceB = Invoice.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .date(LocalDate.of(2026, 3, 21))
                .expenses(List.of(this.expense))
                .incomes(List.of())
                .build();

                InvoiceFindCriteria criteria = new InvoiceFindCriteria();
                when(this.invoicePersistence.findAll(criteria)).thenReturn(Stream.of(invoiceA, invoiceB));

                List<Invoice> invoices = this.invoiceService.findAll(criteria).toList();

        assertEquals(2, invoices.size());
        assertEquals(List.of(invoiceA, invoiceB), invoices);
                verify(this.invoicePersistence).findAll(criteria);
        verifyNoInteractions(this.engagementWebClient);
    }

    @Test
    void shouldReadInvoiceById() {
        this.invoice.setId(UUID.randomUUID());
        when(this.invoicePersistence.readById(this.invoice.getId())).thenReturn(this.invoice);

        Invoice readInvoice = this.invoiceService.readById(this.invoice.getId());

        assertEquals(this.invoice, readInvoice);
        verify(this.invoicePersistence).readById(this.invoice.getId());
        verifyNoInteractions(this.engagementWebClient);
    }

    @Test
    void shouldFailReadInvoiceByIdWhenInvoiceDoesNotExist() {
        UUID invoiceId = UUID.randomUUID();
        when(this.invoicePersistence.readById(invoiceId))
                .thenThrow(new NotFoundException("Invoice id: " + invoiceId));

        NotFoundException thrown = assertThrows(NotFoundException.class,
                () -> this.invoiceService.readById(invoiceId));

        assertEquals("Not Found Exception. Invoice id: " + invoiceId, thrown.getMessage());
        verify(this.invoicePersistence).readById(invoiceId);
        verifyNoInteractions(this.engagementWebClient);
    }

    @Test
    void shouldFindInvoicesByEngagementId() {
        UUID engagementId = UUID.randomUUID();
                InvoiceFindCriteria criteria = new InvoiceFindCriteria(engagementId, null);
        Invoice invoiceA = Invoice.builder()
                .id(UUID.randomUUID())
                .engagementId(engagementId)
                .date(LocalDate.of(2026, 3, 20))
                .expenses(List.of())
                .incomes(List.of(this.income))
                .build();
        Invoice invoiceB = Invoice.builder()
                .id(UUID.randomUUID())
                .engagementId(engagementId)
                .date(LocalDate.of(2026, 3, 21))
                .expenses(List.of(this.expense))
                .incomes(List.of())
                .build();

        when(this.engagementWebClient.readById(engagementId)).thenReturn(new Object());
        when(this.invoicePersistence.findAll(criteria)).thenReturn(Stream.of(invoiceA, invoiceB));

        List<Invoice> invoices = this.invoiceService.findAll(criteria).toList();

        assertEquals(2, invoices.size());
        assertEquals(List.of(invoiceA, invoiceB), invoices);
        verify(this.engagementWebClient).readById(engagementId);
        verify(this.invoicePersistence).findAll(criteria);
    }

    @Test
    void shouldFailFindInvoicesByInvalidEngagementId() {
        UUID engagementId = UUID.randomUUID();
        InvoiceFindCriteria criteria = new InvoiceFindCriteria(engagementId, null);
        RuntimeException exception = new RuntimeException("Engagement not found");
        when(this.engagementWebClient.readById(engagementId)).thenThrow(exception);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> this.invoiceService.findAll(criteria));

        assertEquals("Engagement not found", thrown.getMessage());
        verify(this.engagementWebClient).readById(engagementId);
        verify(this.invoicePersistence, never()).findAll(any());
    }

    @Test
    void shouldFindInvoicesByDate() {
        LocalDate date = LocalDate.of(2026, 3, 21);
        InvoiceFindCriteria criteria = new InvoiceFindCriteria(null, date);
        Invoice invoiceA = Invoice.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .date(date)
                .expenses(List.of())
                .incomes(List.of(this.income))
                .build();
        Invoice invoiceB = Invoice.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .date(date)
                .expenses(List.of(this.expense))
                .incomes(List.of())
                .build();

        when(this.invoicePersistence.findAll(criteria)).thenReturn(Stream.of(invoiceA, invoiceB));

        List<Invoice> invoices = this.invoiceService.findAll(criteria).toList();

        assertEquals(2, invoices.size());
        assertEquals(List.of(invoiceA, invoiceB), invoices);
        verify(this.invoicePersistence).findAll(criteria);
        verifyNoInteractions(this.engagementWebClient);
    }

    @Test
    void shouldFindInvoicesByEngagementIdAndDate() {
        UUID engagementId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 3, 21);
        InvoiceFindCriteria criteria = new InvoiceFindCriteria(engagementId, date);
        Invoice invoiceByCriteria = Invoice.builder()
                .id(UUID.randomUUID())
                .engagementId(engagementId)
                .date(date)
                .expenses(List.of(this.expense))
                .incomes(List.of(this.income))
                .build();

        when(this.engagementWebClient.readById(engagementId)).thenReturn(new Object());
        when(this.invoicePersistence.findAll(criteria)).thenReturn(Stream.of(invoiceByCriteria));

        List<Invoice> invoices = this.invoiceService.findAll(criteria).toList();

        assertEquals(1, invoices.size());
                assertEquals(invoiceByCriteria, invoices.get(0));
        verify(this.engagementWebClient).readById(engagementId);
        verify(this.invoicePersistence).findAll(criteria);
    }

    @Test
    void shouldFindAllInvoicesWhenCriteriaIsEmpty() {
        InvoiceFindCriteria criteria = new InvoiceFindCriteria();
        List<Invoice> expected = List.of(this.invoice);

                when(this.invoicePersistence.findAll(criteria)).thenReturn(Stream.of(this.invoice));

                List<Invoice> result = this.invoiceService.findAll(criteria).toList();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(expected, result);
        verify(this.invoicePersistence).findAll(criteria);
        verifyNoInteractions(this.engagementWebClient);
    }


    @Test
    void shouldFailFindInvoicesByEngagementIdWhenEngagementDoesNotExist() {
        UUID engagementId = UUID.randomUUID();
        InvoiceFindCriteria criteria = new InvoiceFindCriteria();
        criteria.setEngagementId(engagementId);

        when(this.engagementWebClient.readById(engagementId))
                .thenThrow(new NotFoundException("Engagement not found"));

        assertThrows(NotFoundException.class, () -> this.invoiceService.findAll(criteria));
        verify(this.engagementWebClient).readById(engagementId);
        verify(this.invoicePersistence, never()).findAll(any());
    }
}
