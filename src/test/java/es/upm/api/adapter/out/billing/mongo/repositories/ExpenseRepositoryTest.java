package es.upm.api.adapter.out.billing.mongo.repositories;

import es.upm.api.adapter.out.billing.mongo.expense.ExpenseRepository;
import es.upm.api.domain.model.Expense;
import es.upm.api.adapter.out.billing.mongo.expense.ExpenseEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@ActiveProfiles("test")
class ExpenseRepositoryTest {

    @Autowired
    private ExpenseRepository expenseRepository;

    private Expense expense;

    @BeforeEach
    void setUp() {
        this.expenseRepository.deleteAll();
        this.expense = Expense.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(30))
                .date(LocalDate.of(2026, 3, 20))
                .description("Court fee")
                .build();
    }

    @Test
    void shouldSaveExpense() {
        ExpenseEntity expenseEntity = new ExpenseEntity(this.expense);

        ExpenseEntity savedExpenseEntity = this.expenseRepository.save(expenseEntity);

        assertNotNull(savedExpenseEntity);
        assertNotNull(savedExpenseEntity.getId());
        assertEquals(this.expense.getId(), savedExpenseEntity.getId());
        assertEquals(this.expense.getEngagementId(), savedExpenseEntity.getEngagementId());
        assertEquals(this.expense.getAmount(), savedExpenseEntity.getAmount());
        assertEquals(this.expense.getDate(), savedExpenseEntity.getDate());
        assertEquals(this.expense.getDescription(), savedExpenseEntity.getDescription());
    }

    @Test
    void shouldFindExpenseById() {
        ExpenseEntity savedExpenseEntity = this.expenseRepository.save(new ExpenseEntity(this.expense));

        Optional<ExpenseEntity> optionalExpenseEntity = this.expenseRepository.findById(savedExpenseEntity.getId());

        assertTrue(optionalExpenseEntity.isPresent());
        ExpenseEntity foundExpenseEntity = optionalExpenseEntity.get();
        assertEquals(savedExpenseEntity.getId(), foundExpenseEntity.getId());
        assertEquals(savedExpenseEntity.getEngagementId(), foundExpenseEntity.getEngagementId());
        assertEquals(savedExpenseEntity.getAmount(), foundExpenseEntity.getAmount());
        assertEquals(savedExpenseEntity.getDate(), foundExpenseEntity.getDate());
        assertEquals(savedExpenseEntity.getDescription(), foundExpenseEntity.getDescription());
    }
}