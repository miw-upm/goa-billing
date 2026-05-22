package es.upm.api.domain.services;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.criteria.ExpenseFindCriteria;
import es.upm.api.domain.ports.out.billing.ExpenseGateway;
import es.upm.api.adapter.out.engagement.feign.EngagementWebClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class ExpenseServiceIT {

    @Autowired
    private ExpenseService expenseService;

    @MockitoBean
    private ExpenseGateway expenseGateway;

    @MockitoBean
    private EngagementWebClient engagementWebClient;

    private Expense expense;
    private final ExpenseFindCriteria criteria = new ExpenseFindCriteria();

    @BeforeEach
    void setUp() {
        this.expense = Expense.builder()
                .engagementId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(25))
                .date(LocalDate.of(2026, 3, 20))
                .description("Taxi")
                .build();
    }

    @Test
    void shouldCreateExpense() {
        when(this.engagementWebClient.readById(this.expense.getEngagementId())).thenReturn(new Object());

        Expense createdExpense = this.expenseService.create(this.expense);

        assertNotNull(createdExpense);
        assertNotNull(createdExpense.getId());
        assertEquals(this.expense.getEngagementId(), createdExpense.getEngagementId());
        assertEquals(this.expense.getAmount(), createdExpense.getAmount());
        assertEquals(this.expense.getDate(), createdExpense.getDate());
        assertEquals(this.expense.getDescription(), createdExpense.getDescription());

        ArgumentCaptor<Expense> expenseCaptor = ArgumentCaptor.forClass(Expense.class);
        verify(this.expenseGateway).create(expenseCaptor.capture());
        verify(this.engagementWebClient).readById(this.expense.getEngagementId());

        Expense persistedExpense = expenseCaptor.getValue();
        assertNotNull(persistedExpense.getId());
        assertEquals(createdExpense.getId(), persistedExpense.getId());
        assertEquals(this.expense.getEngagementId(), persistedExpense.getEngagementId());
        assertEquals(this.expense.getAmount(), persistedExpense.getAmount());
        assertEquals(this.expense.getDate(), persistedExpense.getDate());
        assertEquals(this.expense.getDescription(), persistedExpense.getDescription());
    }

    @Test
    void shouldNotPersistExpenseWhenEngagementDoesNotExist() {
        RuntimeException exception = new RuntimeException("Engagement not found");
        when(this.engagementWebClient.readById(this.expense.getEngagementId())).thenThrow(exception);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> this.expenseService.create(this.expense));

        assertEquals("Engagement not found", thrown.getMessage());
        verify(this.engagementWebClient).readById(this.expense.getEngagementId());
        verify(this.expenseGateway, never()).create(any());
    }

    @Test
    void shouldReadExpenseById() {
        this.expense.setId(UUID.randomUUID());
        when(this.expenseGateway.readById(this.expense.getId())).thenReturn(this.expense);

        Expense readExpense = this.expenseService.read(this.expense.getId());

        assertEquals(this.expense, readExpense);
        verify(this.expenseGateway).readById(this.expense.getId());
        verifyNoInteractions(this.engagementWebClient);
    }

    @Test
    void shouldFind() {
        Stream<Expense> expenseStream = Stream.of(this.expense);
        when(this.expenseGateway.findAll(this.criteria)).thenReturn(expenseStream);

        Stream<Expense> allExpenses = this.expenseService.find(this.criteria);

        verify(this.expenseGateway).findAll(this.criteria);
        assertEquals(this.expense, allExpenses.findFirst().orElse(null));
    }

    @Test
    void shouldUpdateExpense() {
        UUID id = UUID.randomUUID();
        Expense updateData = Expense.builder()
                .engagementId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(90))
                .description("Updated description")
                .build();

        Expense updatedExpense = Expense.builder()
                .id(id)
                .engagementId(updateData.getEngagementId())
                .amount(updateData.getAmount())
                .date(LocalDate.of(2026, 3, 20))
                .description(updateData.getDescription())
                .build();

        when(this.engagementWebClient.readById(updateData.getEngagementId())).thenReturn(new Object());
        when(this.expenseGateway.update(id, updateData)).thenReturn(updatedExpense);

        Expense response = this.expenseService.update(id, updateData);

        assertEquals(updatedExpense, response);
        verify(this.engagementWebClient).readById(updateData.getEngagementId());
        verify(this.expenseGateway).update(id, updateData);
    }

    @Test
    void shouldNotUpdateExpenseWhenEngagementDoesNotExist() {
        UUID id = UUID.randomUUID();
        Expense updateData = Expense.builder()
                .engagementId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(90))
                .description("Updated description")
                .build();
        RuntimeException exception = new RuntimeException("Engagement not found");
        when(this.engagementWebClient.readById(updateData.getEngagementId())).thenThrow(exception);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> this.expenseService.update(id, updateData));

        assertEquals("Engagement not found", thrown.getMessage());
        verify(this.engagementWebClient).readById(updateData.getEngagementId());
        verify(this.expenseGateway, never()).update(any(), any());
    }
}
