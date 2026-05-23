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
                .vatRate(21)
                .supplier("Taxi Madrid")
                .supplierIdentity("A10000000")
                .taxCategory(TaxCategory.OTROS)
                .issueDate(LocalDate.of(2026, 3, 20))
                .description("Gasto taxi")
                .withholdingTax(BigDecimal.ZERO)
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
        assertEquals(this.expense.getIssueDate(), expenseEntity.getIssueDate());
        assertEquals(this.expense.getDescription(), expenseEntity.getDescription());
        assertEquals(this.expense.getWithholdingTax(), expenseEntity.getWithholdingTax());
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
        expenseEntity.setIssueDate(this.expense.getIssueDate());
        expenseEntity.setDescription(this.expense.getDescription());
        expenseEntity.setWithholdingTax(this.expense.getWithholdingTax());
        expenseEntity.setDocumentPath(this.expense.getDocumentPath());

        Expense mappedExpense = expenseEntity.toDomain();

        assertEquals(expenseEntity.getId(), mappedExpense.getId());
        assertEquals(expenseEntity.getEngagementId(), mappedExpense.getEngagement().getId());
        assertEquals(expenseEntity.getBaseAmount(), mappedExpense.getBaseAmount());
        assertEquals(expenseEntity.getVatRate(), mappedExpense.getVatRate());
        assertEquals(expenseEntity.getSupplier(), mappedExpense.getSupplier());
        assertEquals(expenseEntity.getSupplierIdentity(), mappedExpense.getSupplierIdentity());
        assertEquals(expenseEntity.getTaxCategory(), mappedExpense.getTaxCategory());
        assertEquals(expenseEntity.getIssueDate(), mappedExpense.getIssueDate());
        assertEquals(expenseEntity.getDescription(), mappedExpense.getDescription());
        assertEquals(expenseEntity.getWithholdingTax(), mappedExpense.getWithholdingTax());
        assertEquals(expenseEntity.getDocumentPath(), mappedExpense.getDocumentPath());
    }
}
