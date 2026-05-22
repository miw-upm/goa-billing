package es.upm.api.domain.services;

import es.upm.api.domain.model.*;
import es.upm.api.domain.model.InvoiceOld;
import es.upm.api.domain.model.criteria.InvoiceFindCriteria;
import es.upm.api.domain.ports.out.billing.ExpenseGateway;
import es.upm.api.domain.ports.out.billing.IncomeGateway;
import es.upm.api.domain.ports.out.billing.InvoiceGateway;
import es.upm.api.adapter.out.engagement.feign.EngagementWebClient;
import es.upm.miw.exception.BadRequestException;
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
class InvoiceOldServiceIT {

    @Autowired
    private InvoiceService invoiceService;

    @MockitoBean
    private InvoiceGateway invoiceGateway;

    @MockitoBean
    private ExpenseGateway expenseGateway;

    @MockitoBean
    private IncomeGateway incomeGateway;

    @MockitoBean
    private EngagementWebClient engagementWebClient;

    private InvoiceOld invoiceOld;
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
        this.invoiceOld = InvoiceOld.builder()
                .engagementId(engagementId)
                .date(LocalDate.of(2026, 3, 21))
                .expenses(List.of(Expense.builder().id(this.expense.getId()).build()))
                .incomes(List.of(Income.builder().id(this.income.getId()).build()))
                .build();
    }

    @Test
    void shouldGetInvoiceBreakdown() {
        UUID invoiceId = UUID.randomUUID();
        this.invoiceOld.setId(invoiceId);
        this.invoiceOld.setExpenses(List.of(this.expense));
        this.invoiceOld.setIncomes(List.of(this.income));

        when(this.invoiceGateway.readById(invoiceId)).thenReturn(this.invoiceOld);

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

        verify(this.invoiceGateway).readById(invoiceId);
    }

    @Test
    void whenGetInvoiceBreakdownOfNull_shouldThrowBadRequestException() {
        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> this.invoiceService.getInvoiceBreakdown(null));

        assertEquals("Bad Request Exception. InvoiceOld not found", thrown.getMessage());
        verify(this.invoiceGateway).readById(null);
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
        when(this.engagementWebClient.readById(this.invoiceOld.getEngagementId())).thenReturn(new Object());
        when(this.expenseGateway.read(this.expense.getId())).thenReturn(this.expense);
        when(this.incomeGateway.readById(this.income.getId())).thenReturn(this.income);
        when(this.invoiceGateway.findByExpenseId(this.expense.getId())).thenReturn(null);
        when(this.invoiceGateway.findByIncomeId(this.income.getId())).thenReturn(null);

        InvoiceOld createdInvoiceOld = this.invoiceService.create(this.invoiceOld);

        assertNotNull(createdInvoiceOld);
        assertNotNull(createdInvoiceOld.getId());
        assertEquals(this.invoiceOld.getEngagementId(), createdInvoiceOld.getEngagementId());
        assertEquals(this.invoiceOld.getDate(), createdInvoiceOld.getDate());
        assertEquals(List.of(this.expense), createdInvoiceOld.getExpenses());
        assertEquals(List.of(this.income), createdInvoiceOld.getIncomes());

        ArgumentCaptor<InvoiceOld> invoiceCaptor = ArgumentCaptor.forClass(InvoiceOld.class);
        verify(this.invoiceGateway).create(invoiceCaptor.capture());
        verify(this.engagementWebClient).readById(this.invoiceOld.getEngagementId());
        verify(this.expenseGateway).read(this.expense.getId());
        verify(this.incomeGateway).readById(this.income.getId());

        InvoiceOld persistedInvoiceOld = invoiceCaptor.getValue();
        assertNotNull(persistedInvoiceOld.getId());
        assertEquals(createdInvoiceOld.getId(), persistedInvoiceOld.getId());
        assertEquals(this.invoiceOld.getEngagementId(), persistedInvoiceOld.getEngagementId());
        assertEquals(this.invoiceOld.getDate(), persistedInvoiceOld.getDate());
        assertEquals(List.of(this.expense), persistedInvoiceOld.getExpenses());
        assertEquals(List.of(this.income), persistedInvoiceOld.getIncomes());
    }

    @Test
    void shouldCreateInvoiceWithOnlyExpenses() {
        this.invoiceOld.setIncomes(List.of());
        when(this.engagementWebClient.readById(this.invoiceOld.getEngagementId())).thenReturn(new Object());
        when(this.expenseGateway.read(this.expense.getId())).thenReturn(this.expense);
        when(this.invoiceGateway.findByExpenseId(this.expense.getId())).thenReturn(null);

        InvoiceOld createdInvoiceOld = this.invoiceService.create(this.invoiceOld);

        assertNotNull(createdInvoiceOld.getId());
        assertEquals(List.of(this.expense), createdInvoiceOld.getExpenses());
        assertEquals(0, createdInvoiceOld.getIncomes().size());
        verify(this.engagementWebClient).readById(this.invoiceOld.getEngagementId());
        verify(this.expenseGateway).read(this.expense.getId());
        verify(this.incomeGateway, never()).readById(any());
        verify(this.invoiceGateway).create(any());
    }

    @Test
    void shouldCreateInvoiceWithOnlyIncomes() {
        this.invoiceOld.setExpenses(List.of());
        when(this.engagementWebClient.readById(this.invoiceOld.getEngagementId())).thenReturn(new Object());
        when(this.incomeGateway.readById(this.income.getId())).thenReturn(this.income);
        when(this.invoiceGateway.findByIncomeId(this.income.getId())).thenReturn(null);

        InvoiceOld createdInvoiceOld = this.invoiceService.create(this.invoiceOld);

        assertNotNull(createdInvoiceOld.getId());
        assertEquals(0, createdInvoiceOld.getExpenses().size());
        assertEquals(List.of(this.income), createdInvoiceOld.getIncomes());
        verify(this.engagementWebClient).readById(this.invoiceOld.getEngagementId());
        verify(this.expenseGateway, never()).read(any());
        verify(this.incomeGateway).readById(this.income.getId());
        verify(this.invoiceGateway).create(any());
    }

    @Test
    void shouldNotPersistInvoiceWhenItHasNoExpensesAndNoIncomes() {
        this.invoiceOld.setExpenses(List.of());
        this.invoiceOld.setIncomes(List.of());

        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> this.invoiceService.create(this.invoiceOld));

        assertEquals("Bad Request Exception. InvoiceOld must contain at least one expense or one income", thrown.getMessage());
        verifyNoInteractions(this.engagementWebClient);
        verifyNoInteractions(this.expenseGateway);
        verifyNoInteractions(this.incomeGateway);
        verify(this.invoiceGateway, never()).create(any());
    }

    @Test
    void shouldNotPersistInvoiceWhenDateIsFuture() {
        this.invoiceOld.setDate(LocalDate.now().plusDays(1));

        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> this.invoiceService.create(this.invoiceOld));

        assertEquals("Bad Request Exception. InvoiceOld date cannot be in the future", thrown.getMessage());
        verifyNoInteractions(this.engagementWebClient);
        verifyNoInteractions(this.expenseGateway);
        verifyNoInteractions(this.incomeGateway);
        verify(this.invoiceGateway, never()).create(any());
    }

    @Test
    void shouldNotPersistInvoiceWhenEngagementDoesNotExist() {
        RuntimeException exception = new RuntimeException("Engagement not found");
        when(this.engagementWebClient.readById(this.invoiceOld.getEngagementId())).thenThrow(exception);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> this.invoiceService.create(this.invoiceOld));

        assertEquals("Engagement not found", thrown.getMessage());
        verify(this.engagementWebClient).readById(this.invoiceOld.getEngagementId());
        verify(this.expenseGateway, never()).read(any());
        verify(this.incomeGateway, never()).readById(any());
        verify(this.invoiceGateway, never()).create(any());
    }

    @Test
    void shouldNotPersistInvoiceWhenExpenseDoesNotBelongToEngagement() {
        when(this.engagementWebClient.readById(this.invoiceOld.getEngagementId())).thenReturn(new Object());
        when(this.expenseGateway.read(this.expense.getId())).thenReturn(
                Expense.builder()
                        .id(this.expense.getId())
                        .engagementId(UUID.randomUUID())
                        .amount(this.expense.getAmount())
                        .date(this.expense.getDate())
                        .description(this.expense.getDescription())
                        .build()
        );

        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> this.invoiceService.create(this.invoiceOld));

        assertEquals("Bad Request Exception. Expense does not belong to the invoiceOld engagement", thrown.getMessage());
        verify(this.engagementWebClient).readById(this.invoiceOld.getEngagementId());
        verify(this.expenseGateway).read(this.expense.getId());
        verify(this.incomeGateway, never()).readById(any());
        verify(this.invoiceGateway, never()).create(any());
    }

    @Test
    void shouldNotPersistInvoiceWhenExpenseAlreadyAssignedToAnotherInvoice() {
        when(this.engagementWebClient.readById(this.invoiceOld.getEngagementId())).thenReturn(new Object());
        when(this.expenseGateway.read(this.expense.getId())).thenReturn(this.expense);
        when(this.invoiceGateway.findByExpenseId(this.expense.getId())).thenReturn(InvoiceOld.builder().id(UUID.randomUUID()).build());

        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> this.invoiceService.create(this.invoiceOld));

        assertEquals("Bad Request Exception. Expense is already assigned to another invoiceOld", thrown.getMessage());
        verify(this.engagementWebClient).readById(this.invoiceOld.getEngagementId());
        verify(this.expenseGateway).read(this.expense.getId());
        verify(this.invoiceGateway).findByExpenseId(this.expense.getId());
        verify(this.incomeGateway, never()).readById(any());
        verify(this.invoiceGateway, never()).create(any());
    }

    @Test
    void shouldNotPersistInvoiceWhenIncomeDoesNotBelongToEngagement() {
        when(this.engagementWebClient.readById(this.invoiceOld.getEngagementId())).thenReturn(new Object());
        when(this.expenseGateway.read(this.expense.getId())).thenReturn(this.expense);
        when(this.invoiceGateway.findByExpenseId(this.expense.getId())).thenReturn(null);
        when(this.incomeGateway.readById(this.income.getId())).thenReturn(
                Income.builder()
                        .id(this.income.getId())
                        .engagementId(UUID.randomUUID())
                        .userId(this.income.getUserId())
                        .amount(this.income.getAmount())
                        .date(this.income.getDate())
                        .build()
        );

        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> this.invoiceService.create(this.invoiceOld));

        assertEquals("Bad Request Exception. Income does not belong to the invoiceOld engagement", thrown.getMessage());
        verify(this.engagementWebClient).readById(this.invoiceOld.getEngagementId());
        verify(this.expenseGateway).read(this.expense.getId());
        verify(this.incomeGateway).readById(this.income.getId());
        verify(this.invoiceGateway, never()).create(any());
    }

    @Test
    void shouldNotPersistInvoiceWhenIncomeAlreadyAssignedToAnotherInvoice() {
        when(this.engagementWebClient.readById(this.invoiceOld.getEngagementId())).thenReturn(new Object());
        when(this.expenseGateway.read(this.expense.getId())).thenReturn(this.expense);
        when(this.invoiceGateway.findByExpenseId(this.expense.getId())).thenReturn(null);
        when(this.incomeGateway.readById(this.income.getId())).thenReturn(this.income);
        when(this.invoiceGateway.findByIncomeId(this.income.getId())).thenReturn(InvoiceOld.builder().id(UUID.randomUUID()).build());

        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> this.invoiceService.create(this.invoiceOld));

        assertEquals("Bad Request Exception. Income is already assigned to another invoiceOld", thrown.getMessage());
        verify(this.engagementWebClient).readById(this.invoiceOld.getEngagementId());
        verify(this.expenseGateway).read(this.expense.getId());
        verify(this.invoiceGateway).findByExpenseId(this.expense.getId());
        verify(this.incomeGateway).readById(this.income.getId());
        verify(this.invoiceGateway).findByIncomeId(this.income.getId());
        verify(this.invoiceGateway, never()).create(any());
    }

    @Test
    void shouldUpdateInvoice() {
        UUID invoiceId = UUID.randomUUID();
        this.invoiceOld.setId(invoiceId);
        when(this.invoiceGateway.readById(invoiceId)).thenReturn(this.invoiceOld);
        when(this.engagementWebClient.readById(this.invoiceOld.getEngagementId())).thenReturn(new Object());
        when(this.expenseGateway.read(this.expense.getId())).thenReturn(this.expense);
        when(this.incomeGateway.readById(this.income.getId())).thenReturn(this.income);
        when(this.invoiceGateway.findByExpenseId(this.expense.getId())).thenReturn(InvoiceOld.builder().id(invoiceId).build());
        when(this.invoiceGateway.findByIncomeId(this.income.getId())).thenReturn(InvoiceOld.builder().id(invoiceId).build());
        when(this.invoiceGateway.update(invoiceId, this.invoiceOld)).thenReturn(this.invoiceOld);

        InvoiceOld updatedInvoiceOld = this.invoiceService.update(invoiceId, this.invoiceOld);

        assertEquals(invoiceId, updatedInvoiceOld.getId());
        assertEquals(this.invoiceOld.getEngagementId(), updatedInvoiceOld.getEngagementId());
        assertEquals(this.invoiceOld.getDate(), updatedInvoiceOld.getDate());
        assertEquals(List.of(this.expense), updatedInvoiceOld.getExpenses());
        assertEquals(List.of(this.income), updatedInvoiceOld.getIncomes());
        verify(this.invoiceGateway).readById(invoiceId);
        verify(this.engagementWebClient).readById(this.invoiceOld.getEngagementId());
        verify(this.expenseGateway).read(this.expense.getId());
        verify(this.incomeGateway).readById(this.income.getId());
        verify(this.invoiceGateway).update(invoiceId, this.invoiceOld);
    }

    @Test
    void shouldNotUpdateInvoiceWhenDateIsFuture() {
        UUID invoiceId = UUID.randomUUID();
        this.invoiceOld.setDate(LocalDate.now().plusDays(1));
        when(this.invoiceGateway.readById(invoiceId)).thenReturn(InvoiceOld.builder().id(invoiceId).build());

        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> this.invoiceService.update(invoiceId, this.invoiceOld));

        assertEquals("Bad Request Exception. InvoiceOld date cannot be in the future", thrown.getMessage());
        verify(this.invoiceGateway).readById(invoiceId);
        verifyNoInteractions(this.engagementWebClient);
        verify(this.invoiceGateway, never()).update(any(), any());
    }

    @Test
    void shouldNotUpdateInvoiceWhenItHasNoExpensesAndNoIncomes() {
        UUID invoiceId = UUID.randomUUID();
        this.invoiceOld.setExpenses(List.of());
        this.invoiceOld.setIncomes(List.of());
        when(this.invoiceGateway.readById(invoiceId)).thenReturn(InvoiceOld.builder().id(invoiceId).build());

        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> this.invoiceService.update(invoiceId, this.invoiceOld));

        assertEquals("Bad Request Exception. InvoiceOld must contain at least one expense or one income", thrown.getMessage());
        verify(this.invoiceGateway).readById(invoiceId);
        verifyNoInteractions(this.engagementWebClient);
        verifyNoInteractions(this.expenseGateway);
        verifyNoInteractions(this.incomeGateway);
        verify(this.invoiceGateway, never()).update(any(), any());
    }

    @Test
    void shouldNotUpdateInvoiceWhenItDoesNotExist() {
        UUID invoiceId = UUID.randomUUID();
        when(this.invoiceGateway.readById(invoiceId))
                .thenThrow(new NotFoundException("InvoiceOld id: " + invoiceId));

        NotFoundException thrown = assertThrows(NotFoundException.class,
                () -> this.invoiceService.update(invoiceId, this.invoiceOld));

        assertEquals("Not Found Exception. InvoiceOld id: " + invoiceId, thrown.getMessage());
        verify(this.invoiceGateway).readById(invoiceId);
        verifyNoInteractions(this.engagementWebClient);
        verify(this.invoiceGateway, never()).update(any(), any());
    }

    @Test
    void shouldNotUpdateInvoiceWhenEngagementDoesNotExist() {
        UUID invoiceId = UUID.randomUUID();
        this.invoiceOld.setId(invoiceId);
        RuntimeException exception = new RuntimeException("Engagement not found");
        when(this.invoiceGateway.readById(invoiceId)).thenReturn(this.invoiceOld);
        when(this.engagementWebClient.readById(this.invoiceOld.getEngagementId())).thenThrow(exception);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> this.invoiceService.update(invoiceId, this.invoiceOld));

        assertEquals("Engagement not found", thrown.getMessage());
        verify(this.invoiceGateway).readById(invoiceId);
        verify(this.engagementWebClient).readById(this.invoiceOld.getEngagementId());
        verify(this.expenseGateway, never()).read(any());
        verify(this.incomeGateway, never()).readById(any());
        verify(this.invoiceGateway, never()).update(any(), any());
    }

    @Test
    void shouldNotUpdateInvoiceWhenExpenseDoesNotBelongToEngagement() {
        UUID invoiceId = UUID.randomUUID();
        this.invoiceOld.setId(invoiceId);
        when(this.invoiceGateway.readById(invoiceId)).thenReturn(this.invoiceOld);
        when(this.engagementWebClient.readById(this.invoiceOld.getEngagementId())).thenReturn(new Object());
        when(this.expenseGateway.read(this.expense.getId())).thenReturn(
                Expense.builder()
                        .id(this.expense.getId())
                        .engagementId(UUID.randomUUID())
                        .amount(this.expense.getAmount())
                        .date(this.expense.getDate())
                        .description(this.expense.getDescription())
                        .build()
        );

        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> this.invoiceService.update(invoiceId, this.invoiceOld));

        assertEquals("Bad Request Exception. Expense does not belong to the invoiceOld engagement", thrown.getMessage());
        verify(this.invoiceGateway).readById(invoiceId);
        verify(this.engagementWebClient).readById(this.invoiceOld.getEngagementId());
        verify(this.expenseGateway).read(this.expense.getId());
        verify(this.incomeGateway, never()).readById(any());
        verify(this.invoiceGateway, never()).update(any(), any());
    }

    @Test
    void shouldNotUpdateInvoiceWhenIncomeDoesNotBelongToEngagement() {
        UUID invoiceId = UUID.randomUUID();
        this.invoiceOld.setId(invoiceId);
        when(this.invoiceGateway.readById(invoiceId)).thenReturn(this.invoiceOld);
        when(this.engagementWebClient.readById(this.invoiceOld.getEngagementId())).thenReturn(new Object());
        when(this.expenseGateway.read(this.expense.getId())).thenReturn(this.expense);
        when(this.invoiceGateway.findByExpenseId(this.expense.getId())).thenReturn(InvoiceOld.builder().id(invoiceId).build());
        when(this.incomeGateway.readById(this.income.getId())).thenReturn(
                Income.builder()
                        .id(this.income.getId())
                        .engagementId(UUID.randomUUID())
                        .userId(this.income.getUserId())
                        .amount(this.income.getAmount())
                        .date(this.income.getDate())
                        .build()
        );

        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> this.invoiceService.update(invoiceId, this.invoiceOld));

        assertEquals("Bad Request Exception. Income does not belong to the invoiceOld engagement", thrown.getMessage());
        verify(this.invoiceGateway).readById(invoiceId);
        verify(this.engagementWebClient).readById(this.invoiceOld.getEngagementId());
        verify(this.expenseGateway).read(this.expense.getId());
        verify(this.incomeGateway).readById(this.income.getId());
        verify(this.invoiceGateway, never()).update(any(), any());
    }

    @Test
    void shouldNotUpdateInvoiceWhenExpenseAlreadyAssignedToAnotherInvoice() {
        UUID invoiceId = UUID.randomUUID();
        this.invoiceOld.setId(invoiceId);
        when(this.invoiceGateway.readById(invoiceId)).thenReturn(this.invoiceOld);
        when(this.engagementWebClient.readById(this.invoiceOld.getEngagementId())).thenReturn(new Object());
        when(this.expenseGateway.read(this.expense.getId())).thenReturn(this.expense);
        when(this.invoiceGateway.findByExpenseId(this.expense.getId())).thenReturn(InvoiceOld.builder().id(UUID.randomUUID()).build());

        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> this.invoiceService.update(invoiceId, this.invoiceOld));

        assertEquals("Bad Request Exception. Expense is already assigned to another invoiceOld", thrown.getMessage());
        verify(this.invoiceGateway).readById(invoiceId);
        verify(this.engagementWebClient).readById(this.invoiceOld.getEngagementId());
        verify(this.expenseGateway).read(this.expense.getId());
        verify(this.invoiceGateway).findByExpenseId(this.expense.getId());
        verify(this.incomeGateway, never()).readById(any());
        verify(this.invoiceGateway, never()).update(any(), any());
    }

    @Test
    void shouldNotUpdateInvoiceWhenIncomeAlreadyAssignedToAnotherInvoice() {
        UUID invoiceId = UUID.randomUUID();
        this.invoiceOld.setId(invoiceId);
        when(this.invoiceGateway.readById(invoiceId)).thenReturn(this.invoiceOld);
        when(this.engagementWebClient.readById(this.invoiceOld.getEngagementId())).thenReturn(new Object());
        when(this.expenseGateway.read(this.expense.getId())).thenReturn(this.expense);
        when(this.invoiceGateway.findByExpenseId(this.expense.getId())).thenReturn(InvoiceOld.builder().id(invoiceId).build());
        when(this.incomeGateway.readById(this.income.getId())).thenReturn(this.income);
        when(this.invoiceGateway.findByIncomeId(this.income.getId())).thenReturn(InvoiceOld.builder().id(UUID.randomUUID()).build());

        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> this.invoiceService.update(invoiceId, this.invoiceOld));

        assertEquals("Bad Request Exception. Income is already assigned to another invoiceOld", thrown.getMessage());
        verify(this.invoiceGateway).readById(invoiceId);
        verify(this.engagementWebClient).readById(this.invoiceOld.getEngagementId());
        verify(this.expenseGateway).read(this.expense.getId());
        verify(this.incomeGateway).readById(this.income.getId());
        verify(this.invoiceGateway).findByIncomeId(this.income.getId());
        verify(this.invoiceGateway, never()).update(any(), any());
    }

    @Test
    void shouldFindAllInvoices() {
        InvoiceOld invoiceOldA = InvoiceOld.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .date(LocalDate.of(2026, 3, 20))
                .expenses(List.of())
                .incomes(List.of(this.income))
                .build();
        InvoiceOld invoiceOldB = InvoiceOld.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .date(LocalDate.of(2026, 3, 21))
                .expenses(List.of(this.expense))
                .incomes(List.of())
                .build();

                InvoiceFindCriteria criteria = new InvoiceFindCriteria();
                when(this.invoiceGateway.findAll(criteria)).thenReturn(Stream.of(invoiceOldA, invoiceOldB));

                List<InvoiceOld> invoiceOlds = this.invoiceService.findAll(criteria).toList();

        assertEquals(2, invoiceOlds.size());
        assertEquals(List.of(invoiceOldA, invoiceOldB), invoiceOlds);
                verify(this.invoiceGateway).findAll(criteria);
        verifyNoInteractions(this.engagementWebClient);
    }

    @Test
    void shouldReadInvoiceById() {
        this.invoiceOld.setId(UUID.randomUUID());
        when(this.invoiceGateway.readById(this.invoiceOld.getId())).thenReturn(this.invoiceOld);

        InvoiceOld readInvoiceOld = this.invoiceService.readById(this.invoiceOld.getId());

        assertEquals(this.invoiceOld, readInvoiceOld);
        verify(this.invoiceGateway).readById(this.invoiceOld.getId());
        verifyNoInteractions(this.engagementWebClient);
    }

    @Test
    void shouldFailReadInvoiceByIdWhenInvoiceDoesNotExist() {
        UUID invoiceId = UUID.randomUUID();
        when(this.invoiceGateway.readById(invoiceId))
                .thenThrow(new NotFoundException("InvoiceOld id: " + invoiceId));

        NotFoundException thrown = assertThrows(NotFoundException.class,
                () -> this.invoiceService.readById(invoiceId));

        assertEquals("Not Found Exception. InvoiceOld id: " + invoiceId, thrown.getMessage());
        verify(this.invoiceGateway).readById(invoiceId);
        verifyNoInteractions(this.engagementWebClient);
    }

    @Test
    void shouldFindInvoicesByEngagementId() {
        UUID engagementId = UUID.randomUUID();
                InvoiceFindCriteria criteria = new InvoiceFindCriteria(engagementId, null);
        InvoiceOld invoiceOldA = InvoiceOld.builder()
                .id(UUID.randomUUID())
                .engagementId(engagementId)
                .date(LocalDate.of(2026, 3, 20))
                .expenses(List.of())
                .incomes(List.of(this.income))
                .build();
        InvoiceOld invoiceOldB = InvoiceOld.builder()
                .id(UUID.randomUUID())
                .engagementId(engagementId)
                .date(LocalDate.of(2026, 3, 21))
                .expenses(List.of(this.expense))
                .incomes(List.of())
                .build();

        when(this.engagementWebClient.readById(engagementId)).thenReturn(new Object());
        when(this.invoiceGateway.findAll(criteria)).thenReturn(Stream.of(invoiceOldA, invoiceOldB));

        List<InvoiceOld> invoiceOlds = this.invoiceService.findAll(criteria).toList();

        assertEquals(2, invoiceOlds.size());
        assertEquals(List.of(invoiceOldA, invoiceOldB), invoiceOlds);
        verify(this.engagementWebClient).readById(engagementId);
        verify(this.invoiceGateway).findAll(criteria);
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
        verify(this.invoiceGateway, never()).findAll(any());
    }

    @Test
    void shouldFindInvoicesByDate() {
        LocalDate date = LocalDate.of(2026, 3, 21);
        InvoiceFindCriteria criteria = new InvoiceFindCriteria(null, date);
        InvoiceOld invoiceOldA = InvoiceOld.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .date(date)
                .expenses(List.of())
                .incomes(List.of(this.income))
                .build();
        InvoiceOld invoiceOldB = InvoiceOld.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .date(date)
                .expenses(List.of(this.expense))
                .incomes(List.of())
                .build();

        when(this.invoiceGateway.findAll(criteria)).thenReturn(Stream.of(invoiceOldA, invoiceOldB));

        List<InvoiceOld> invoiceOlds = this.invoiceService.findAll(criteria).toList();

        assertEquals(2, invoiceOlds.size());
        assertEquals(List.of(invoiceOldA, invoiceOldB), invoiceOlds);
        verify(this.invoiceGateway).findAll(criteria);
        verifyNoInteractions(this.engagementWebClient);
    }

    @Test
    void shouldFindInvoicesByEngagementIdAndDate() {
        UUID engagementId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 3, 21);
        InvoiceFindCriteria criteria = new InvoiceFindCriteria(engagementId, date);
        InvoiceOld invoiceOldByCriteria = InvoiceOld.builder()
                .id(UUID.randomUUID())
                .engagementId(engagementId)
                .date(date)
                .expenses(List.of(this.expense))
                .incomes(List.of(this.income))
                .build();

        when(this.engagementWebClient.readById(engagementId)).thenReturn(new Object());
        when(this.invoiceGateway.findAll(criteria)).thenReturn(Stream.of(invoiceOldByCriteria));

        List<InvoiceOld> invoiceOlds = this.invoiceService.findAll(criteria).toList();

        assertEquals(1, invoiceOlds.size());
                assertEquals(invoiceOldByCriteria, invoiceOlds.get(0));
        verify(this.engagementWebClient).readById(engagementId);
        verify(this.invoiceGateway).findAll(criteria);
    }

    @Test
    void shouldFindAllInvoicesWhenCriteriaIsEmpty() {
        InvoiceFindCriteria criteria = new InvoiceFindCriteria();
        List<InvoiceOld> expected = List.of(this.invoiceOld);

                when(this.invoiceGateway.findAll(criteria)).thenReturn(Stream.of(this.invoiceOld));

                List<InvoiceOld> result = this.invoiceService.findAll(criteria).toList();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(expected, result);
        verify(this.invoiceGateway).findAll(criteria);
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
        verify(this.invoiceGateway, never()).findAll(any());
    }
}
