package es.upm.api.domain.services;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.persistence.ExpensePersistence;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class ExpenseServiceIT {

    @Autowired
    private ExpenseService expenseService;

    @MockitoBean
    private ExpensePersistence expensePersistence;

    @MockitoBean
    private EngagementWebClient engagementWebClient;

    private Expense expense;

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
        verify(this.expensePersistence).create(expenseCaptor.capture());
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
        verify(this.expensePersistence, never()).create(any());
    }

    @Test
    void shouldReadExpenseById() {
        UUID id = UUID.randomUUID();
        Expense persistedExpense = Expense.builder()
                .id(id)
                .engagementId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(35))
                .date(LocalDate.of(2026, 3, 21))
                .description("Meal")
                .build();
        when(this.expensePersistence.readById(id)).thenReturn(persistedExpense);

        Expense readExpense = this.expenseService.readById(id);

        assertEquals(persistedExpense, readExpense);
        verify(this.expensePersistence).readById(id);
        verifyNoInteractions(this.engagementWebClient);
    }
}
