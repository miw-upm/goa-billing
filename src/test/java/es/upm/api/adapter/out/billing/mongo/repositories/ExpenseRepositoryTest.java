package es.upm.api.adapter.out.billing.mongo.repositories;

import es.upm.api.adapter.out.billing.mongo.expense.ExpenseEntity;
import es.upm.api.adapter.out.billing.mongo.expense.ExpenseRepository;
import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.SupplierInfo;
import es.upm.api.domain.model.TaxCategory;
import es.upm.api.domain.model.external.EngagementSnapshot;
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
                .engagement(EngagementSnapshot.builder().id(UUID.randomUUID()).build())
                .baseAmount(BigDecimal.valueOf(30))
                .vatRate(21)
                .supplier(SupplierInfo.builder().name("Court services").identity("E50000000").build())
                .taxCategory(TaxCategory.SERVICIOS_PROFESIONALES)
                .issueDate(LocalDate.of(2026, 3, 20))
                .withholdingTax(BigDecimal.ZERO)
                .documentPath("docs/court.pdf")
                .build();
    }

    @Test
    void shouldSaveExpense() {
        ExpenseEntity expenseEntity = new ExpenseEntity(this.expense);

        ExpenseEntity savedExpenseEntity = this.expenseRepository.save(expenseEntity);

        assertNotNull(savedExpenseEntity);
        assertNotNull(savedExpenseEntity.getId());
        assertEquals(this.expense.getId().toString(), savedExpenseEntity.getId());
        assertEquals(this.expense.getEngagement().getId().toString(), savedExpenseEntity.getEngagementId());
        assertEquals(this.expense.getBaseAmount(), savedExpenseEntity.getBaseAmount());
        assertEquals(this.expense.getVatRate(), savedExpenseEntity.getVatRate());
        assertEquals(this.expense.getSupplier(), savedExpenseEntity.getSupplier());
        assertEquals(this.expense.getTaxCategory(), savedExpenseEntity.getTaxCategory());
        assertEquals(this.expense.getIssueDate(), savedExpenseEntity.getIssueDate());
        assertEquals(this.expense.getDocumentPath(), savedExpenseEntity.getDocumentPath());
    }

    @Test
    void shouldFindExpenseById() {
        ExpenseEntity savedExpenseEntity = this.expenseRepository.save(new ExpenseEntity(this.expense));

        Optional<ExpenseEntity> optionalExpenseEntity = this.expenseRepository.findById(savedExpenseEntity.getId());

        assertTrue(optionalExpenseEntity.isPresent());
        ExpenseEntity foundExpenseEntity = optionalExpenseEntity.get();
        assertEquals(savedExpenseEntity.getId(), foundExpenseEntity.getId());
        assertEquals(savedExpenseEntity.getEngagementId(), foundExpenseEntity.getEngagementId());
        assertEquals(savedExpenseEntity.getBaseAmount(), foundExpenseEntity.getBaseAmount());
        assertEquals(savedExpenseEntity.getVatRate(), foundExpenseEntity.getVatRate());
        assertEquals(savedExpenseEntity.getSupplier(), foundExpenseEntity.getSupplier());
        assertEquals(savedExpenseEntity.getTaxCategory(), foundExpenseEntity.getTaxCategory());
        assertEquals(savedExpenseEntity.getIssueDate(), foundExpenseEntity.getIssueDate());
        assertEquals(savedExpenseEntity.getDocumentPath(), foundExpenseEntity.getDocumentPath());
    }
}
