package es.upm.api.adapter.out.billing.mongo.entities;

import es.upm.api.adapter.out.billing.mongo.expense.ExpenseEntity;
import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.TaxCategory;
import es.upm.api.domain.model.external.EngagementSnapshot;
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
                .engagement(EngagementSnapshot.builder().id(UUID.randomUUID()).build())
                .baseAmount(BigDecimal.valueOf(25))
                .vatRate(BigDecimal.valueOf(21))
                .supplier("Taxi Madrid")
                .supplierIdentity("A10000000")
                .taxCategory(TaxCategory.OTROS)
                .issueDate(LocalDate.of(2026, 3, 20))
                .documentPath("doc/path")
                .build();
    }

    @Test
    void shouldBuildExpenseEntityFromExpense() {
        ExpenseEntity expenseEntity = new ExpenseEntity(this.expense);

        assertEquals(this.expense.getId(), expenseEntity.getId());
        assertEquals(this.expense.getEngagement().getId(), expenseEntity.getEngagementId());
        assertEquals(this.expense.getBaseAmount(), expenseEntity.getBaseAmount());
        assertEquals(this.expense.getVatRate(), expenseEntity.getVatRate());
        assertEquals(this.expense.getSupplier(), expenseEntity.getSupplier());
        assertEquals(this.expense.getSupplierIdentity(), expenseEntity.getSupplierIdentity());
        assertEquals(this.expense.getTaxCategory(), expenseEntity.getTaxCategory());
        assertEquals(this.expense.getIssueDate(), expenseEntity.getDate());
        assertEquals(this.expense.getDocumentPath(), expenseEntity.getDocumentPath());
    }

    @Test
    void shouldConvertExpenseEntityToDomain() {
        ExpenseEntity expenseEntity = new ExpenseEntity();
        expenseEntity.setId(this.expense.getId());
        expenseEntity.setEngagementId(this.expense.getEngagement().getId());
        expenseEntity.setBaseAmount(this.expense.getBaseAmount());
        expenseEntity.setVatRate(this.expense.getVatRate());
        expenseEntity.setSupplier(this.expense.getSupplier());
        expenseEntity.setSupplierIdentity(this.expense.getSupplierIdentity());
        expenseEntity.setTaxCategory(this.expense.getTaxCategory());
        expenseEntity.setDate(this.expense.getIssueDate());
        expenseEntity.setDocumentPath(this.expense.getDocumentPath());

        Expense mappedExpense = expenseEntity.toDomain();

        assertEquals(expenseEntity.getId(), mappedExpense.getId());
        assertEquals(expenseEntity.getEngagementId(), mappedExpense.getEngagement().getId());
        assertEquals(expenseEntity.getBaseAmount(), mappedExpense.getBaseAmount());
        assertEquals(expenseEntity.getVatRate(), mappedExpense.getVatRate());
        assertEquals(expenseEntity.getSupplier(), mappedExpense.getSupplier());
        assertEquals(expenseEntity.getSupplierIdentity(), mappedExpense.getSupplierIdentity());
        assertEquals(expenseEntity.getTaxCategory(), mappedExpense.getTaxCategory());
        assertEquals(expenseEntity.getDate(), mappedExpense.getIssueDate());
        assertEquals(expenseEntity.getDocumentPath(), mappedExpense.getDocumentPath());
    }
}

