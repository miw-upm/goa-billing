package es.upm.api.infrastructure.mongodb.entities;

import es.upm.api.domain.model.Expense;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpenseEntityTest {

    private Expense expense;

    @BeforeEach
    void setUp() {
        this.expense = Expense.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(25))
                .date(LocalDate.of(2026, 3, 20))
                .description("Taxi")
                .build();
    }

    @Test
    void shouldBuildExpenseEntityFromExpense() {
        ExpenseEntity expenseEntity = new ExpenseEntity(this.expense);

        assertEquals(this.expense.getId(), expenseEntity.getId());
        assertEquals(this.expense.getEngagementId(), expenseEntity.getEngagementId());
        assertEquals(this.expense.getAmount(), expenseEntity.getAmount());
        assertEquals(this.expense.getDate(), expenseEntity.getDate());
        assertEquals(this.expense.getDescription(), expenseEntity.getDescription());
    }

    @Test
    void shouldConvertExpenseEntityToExpense() {
        ExpenseEntity expenseEntity = new ExpenseEntity();
        expenseEntity.setId(this.expense.getId());
        expenseEntity.setEngagementId(this.expense.getEngagementId());
        expenseEntity.setAmount(this.expense.getAmount());
        expenseEntity.setDate(this.expense.getDate());
        expenseEntity.setDescription(this.expense.getDescription());

        Expense mappedExpense = expenseEntity.toExpense();

        assertEquals(expenseEntity.getId(), mappedExpense.getId());
        assertEquals(expenseEntity.getEngagementId(), mappedExpense.getEngagementId());
        assertEquals(expenseEntity.getAmount(), mappedExpense.getAmount());
        assertEquals(expenseEntity.getDate(), mappedExpense.getDate());
        assertEquals(expenseEntity.getDescription(), mappedExpense.getDescription());
    }
}

