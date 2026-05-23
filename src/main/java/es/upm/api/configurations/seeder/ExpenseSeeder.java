package es.upm.api.configurations.seeder;

import es.upm.api.adapter.out.billing.mongo.expense.ExpenseEntity;
import es.upm.api.adapter.out.billing.mongo.expense.ExpenseRepository;
import es.upm.api.domain.model.SupplierInfo;
import es.upm.api.domain.model.TaxCategory;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Log4j2
@Repository
@Profile({"dev", "test"})
public class ExpenseSeeder {

    private final ExpenseRepository expenseRepository;

    public ExpenseSeeder(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    public void seedDatabase() {
        log.warn("------- Expense Initial Load -----------");
        List<ExpenseEntity> expenses = List.of(
                this.buildExpense(
                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff1000",
                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0000",
                        "35.50", 21, "Taxi Madrid", "A10000000", TaxCategory.OTROS,
                        LocalDate.of(2026, 3, 15),
                        null
                ),
                this.buildExpense(
                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff1001",
                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0001",
                        "120.00", 21, "Hotel Central", "B20000000", TaxCategory.SERVICIOS_PROFESIONALES,
                        LocalDate.of(2026, 3, 16),
                        null
                ),
                this.buildExpense(
                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff1002",
                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0000",
                        "18.90", 21, "Restaurante Norte", "C30000000", TaxCategory.MANUTENCION,
                        LocalDate.of(2026, 3, 17),
                        null
                ),
                this.buildExpense(
                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff1004",
                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0001",
                        "64.80", 21, "Restaurante Sur", "D40000000", TaxCategory.MANUTENCION,
                        LocalDate.of(2026, 3, 18),
                        null
                )
        );
        this.expenseRepository.saveAll(expenses);
    }

    public void deleteAll() {
        this.expenseRepository.deleteAll();
    }

    private ExpenseEntity buildExpense(String id, String engagementId, String baseAmount, Integer vatRate,
                                       String supplier, String supplierIdentity, TaxCategory taxCategory,
                                       LocalDate date, String documentPath) {
        ExpenseEntity expenseEntity = new ExpenseEntity();
        expenseEntity.setId(UUID.fromString(id));
        expenseEntity.setEngagementId(UUID.fromString(engagementId));
        expenseEntity.setBaseAmount(new BigDecimal(baseAmount));
        expenseEntity.setVatRate(vatRate);
        expenseEntity.setSupplier(SupplierInfo.builder()
                .name(supplier)
                .identity(supplierIdentity)
                .build());
        expenseEntity.setTaxCategory(taxCategory);
        expenseEntity.setIssueDate(date);
        expenseEntity.setWithholdingTax(BigDecimal.ZERO);
        expenseEntity.setDocumentPath(documentPath);
        return expenseEntity;
    }
}
