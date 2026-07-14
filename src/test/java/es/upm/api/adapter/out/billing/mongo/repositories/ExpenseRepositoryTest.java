package es.upm.api.adapter.out.billing.mongo.repositories;

import es.upm.api.adapter.out.billing.mongo.expense.ExpenseEntity;
import es.upm.api.adapter.out.billing.mongo.expense.ExpenseRepository;
import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.SupplierInfo;
import es.upm.api.domain.model.TaxCategory;
import es.upm.api.domain.model.external.EngagementSnapshot;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataMongoTest
@ActiveProfiles("test")
class ExpenseRepositoryTest {
    private static final Decimal128 INVESTMENT_ASSET_THRESHOLD = Decimal128.parse("3005.06");

    @Autowired
    private ExpenseRepository expenseRepository;

    private Expense firstExpense;
    private Expense secondExpense;
    private Expense thirdExpense;

    @BeforeEach
    void setUp() {
        this.expenseRepository.deleteAll();
        UUID engagementId = UUID.randomUUID();
        this.firstExpense = Expense.builder()
                .id(UUID.randomUUID())
                .series("2026")
                .number(1)
                .engagement(EngagementSnapshot.builder().id(engagementId).build())
                .baseAmount(BigDecimal.valueOf(30))
                .vatRate(21)
                .supplier(SupplierInfo.builder().name("Taxi Madrid").identity("A10000000").build())
                .taxCategory(TaxCategory.OTROS)
                .depreciationRate(100)
                .issueDate(LocalDate.of(2026, 3, 20))
                .withholdingTax(BigDecimal.ZERO)
                .documentPath("docs/taxi-1.pdf")
                .build();
        this.secondExpense = Expense.builder()
                .id(UUID.randomUUID())
                .series("2026")
                .number(2)
                .engagement(EngagementSnapshot.builder().id(engagementId).build())
                .baseAmount(BigDecimal.valueOf(35))
                .vatRate(21)
                .supplier(SupplierInfo.builder().name("Taxi Centro").identity("A10000001").build())
                .taxCategory(TaxCategory.OTROS)
                .depreciationRate(100)
                .issueDate(LocalDate.of(2026, 3, 22))
                .withholdingTax(BigDecimal.ZERO)
                .documentPath("docs/taxi-2.pdf")
                .build();
        this.thirdExpense = Expense.builder()
                .id(UUID.randomUUID())
                .series("2025")
                .number(7)
                .engagement(EngagementSnapshot.builder().id(UUID.randomUUID()).build())
                .baseAmount(BigDecimal.valueOf(300))
                .vatRate(21)
                .supplier(SupplierInfo.builder().name("Court services").identity("E50000000").build())
                .taxCategory(TaxCategory.SERVICIOS_PROFESIONALES)
                .depreciationRate(10)
                .issueDate(LocalDate.of(2026, 3, 18))
                .withholdingTax(BigDecimal.ZERO)
                .documentPath("docs/court.pdf")
                .build();
        this.expenseRepository.saveAll(List.of(
                new ExpenseEntity(this.firstExpense),
                new ExpenseEntity(this.secondExpense),
                new ExpenseEntity(this.thirdExpense)
        ));
    }

    @Test
    void shouldFindBySupplierContainsOrderBySeriesDescNumberDesc() {
        List<ExpenseEntity> result = this.expenseRepository
                .findBySupplierNameContainingIgnoreCaseOrSupplierIdentityContainingIgnoreCaseOrderBySeriesDescNumberDesc(
                        "taxi", "taxi");

        assertEquals(2, result.size());
        assertEquals(this.secondExpense.getId().toString(), result.getFirst().getId());
        assertEquals(this.firstExpense.getId().toString(), result.get(1).getId());
    }

    @Test
    void shouldFindByEngagementIdPrefixOrderBySeriesDescNumberDesc() {
        String prefix = this.firstExpense.getEngagement().getId().toString().substring(0, 4);
        List<ExpenseEntity> result = this.expenseRepository.findByEngagementIdStartingWithOrderBySeriesDescNumberDesc(prefix);

        assertEquals(2, result.size());
        assertEquals(this.secondExpense.getId().toString(), result.getFirst().getId());
        assertEquals(this.firstExpense.getId().toString(), result.get(1).getId());
    }

    @Test
    void shouldFindReceivedBookWithVat() {
        Expense zeroVatExpense = Expense.builder()
                .id(UUID.randomUUID())
                .series("2026")
                .number(3)
                .baseAmount(BigDecimal.valueOf(50))
                .vatRate(0)
                .supplier(SupplierInfo.builder().name("No Vat").identity("N10000000").build())
                .taxCategory(TaxCategory.OTROS)
                .depreciationRate(100)
                .issueDate(LocalDate.of(2026, 3, 23))
                .withholdingTax(BigDecimal.ZERO)
                .build();
        Expense excludedInvestmentAssetExpense = Expense.builder()
                .id(UUID.randomUUID())
                .series("2026")
                .number(4)
                .baseAmount(BigDecimal.valueOf(4000))
                .vatRate(21)
                .supplier(SupplierInfo.builder().name("Investment").identity("I10000000").build())
                .taxCategory(TaxCategory.OTROS)
                .depreciationRate(10)
                .issueDate(LocalDate.of(2026, 3, 19))
                .withholdingTax(BigDecimal.ZERO)
                .build();
        this.expenseRepository.saveAll(List.of(new ExpenseEntity(zeroVatExpense), new ExpenseEntity(excludedInvestmentAssetExpense)));

        List<ExpenseEntity> result = this.expenseRepository.findReceivedBookWithVat(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), INVESTMENT_ASSET_THRESHOLD);

        assertEquals(3, result.size());
        assertEquals(this.thirdExpense.getId().toString(), result.getFirst().getId());
        assertEquals(this.firstExpense.getId().toString(), result.get(1).getId());
        assertEquals(this.secondExpense.getId().toString(), result.get(2).getId());
    }

    @Test
    void shouldFindReceivedBookWithVatByNumberRange() {
        Expense zeroVatExpense = Expense.builder()
                .id(UUID.randomUUID())
                .series("2026")
                .number(3)
                .baseAmount(BigDecimal.valueOf(50))
                .vatRate(0)
                .supplier(SupplierInfo.builder().name("No Vat").identity("N10000000").build())
                .taxCategory(TaxCategory.OTROS)
                .depreciationRate(100)
                .issueDate(LocalDate.of(2026, 3, 23))
                .withholdingTax(BigDecimal.ZERO)
                .build();
        Expense excludedInvestmentAssetExpense = Expense.builder()
                .id(UUID.randomUUID())
                .series("2026")
                .number(4)
                .baseAmount(BigDecimal.valueOf(4000))
                .vatRate(21)
                .supplier(SupplierInfo.builder().name("Investment").identity("I10000000").build())
                .taxCategory(TaxCategory.OTROS)
                .depreciationRate(10)
                .issueDate(LocalDate.of(2026, 3, 19))
                .withholdingTax(BigDecimal.ZERO)
                .build();
        Expense smallInvestmentExpense = Expense.builder()
                .id(UUID.randomUUID())
                .series("2026")
                .number(5)
                .baseAmount(BigDecimal.valueOf(300))
                .vatRate(21)
                .supplier(SupplierInfo.builder().name("Small Investment").identity("I20000000").build())
                .taxCategory(TaxCategory.OTROS)
                .depreciationRate(10)
                .issueDate(LocalDate.of(2026, 3, 24))
                .withholdingTax(BigDecimal.ZERO)
                .build();
        this.expenseRepository.saveAll(List.of(
                new ExpenseEntity(zeroVatExpense),
                new ExpenseEntity(excludedInvestmentAssetExpense),
                new ExpenseEntity(smallInvestmentExpense)
        ));

        List<ExpenseEntity> result = this.expenseRepository.findReceivedBookWithVat(
                "2026", 1, 5, INVESTMENT_ASSET_THRESHOLD);

        assertEquals(3, result.size());
        assertEquals(this.firstExpense.getId().toString(), result.getFirst().getId());
        assertEquals(this.secondExpense.getId().toString(), result.get(1).getId());
        assertEquals(smallInvestmentExpense.getId().toString(), result.get(2).getId());
    }

    @Test
    void shouldFindCurrentExpensesBookExcludingInvestments() {
        Expense zeroVatExpense = Expense.builder()
                .id(UUID.randomUUID())
                .series("2026")
                .number(3)
                .baseAmount(BigDecimal.valueOf(50))
                .vatRate(0)
                .supplier(SupplierInfo.builder().name("No Vat").identity("N10000000").build())
                .taxCategory(TaxCategory.OTROS)
                .depreciationRate(100)
                .issueDate(LocalDate.of(2026, 3, 23))
                .withholdingTax(BigDecimal.ZERO)
                .build();
        this.expenseRepository.save(new ExpenseEntity(zeroVatExpense));

        List<ExpenseEntity> result = this.expenseRepository.findCurrentExpensesBook(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));

        assertEquals(3, result.size());
        assertEquals(this.firstExpense.getId().toString(), result.getFirst().getId());
        assertEquals(this.secondExpense.getId().toString(), result.get(1).getId());
        assertEquals(zeroVatExpense.getId().toString(), result.get(2).getId());
    }

    @Test
    void shouldFindCurrentExpensesBookByNumberRangeExcludingInvestments() {
        Expense zeroVatExpense = Expense.builder()
                .id(UUID.randomUUID())
                .series("2026")
                .number(3)
                .baseAmount(BigDecimal.valueOf(50))
                .vatRate(0)
                .supplier(SupplierInfo.builder().name("No Vat").identity("N10000000").build())
                .taxCategory(TaxCategory.OTROS)
                .depreciationRate(100)
                .issueDate(LocalDate.of(2026, 3, 23))
                .withholdingTax(BigDecimal.ZERO)
                .build();
        Expense investmentAssetExpense = Expense.builder()
                .id(UUID.randomUUID())
                .series("2026")
                .number(4)
                .baseAmount(BigDecimal.valueOf(4000))
                .vatRate(21)
                .supplier(SupplierInfo.builder().name("Investment").identity("I10000000").build())
                .taxCategory(TaxCategory.OTROS)
                .depreciationRate(10)
                .issueDate(LocalDate.of(2026, 3, 24))
                .withholdingTax(BigDecimal.ZERO)
                .build();
        this.expenseRepository.saveAll(List.of(new ExpenseEntity(zeroVatExpense), new ExpenseEntity(investmentAssetExpense)));

        List<ExpenseEntity> result = this.expenseRepository.findCurrentExpensesBook("2026", 1, 4);

        assertEquals(3, result.size());
        assertEquals(this.firstExpense.getId().toString(), result.getFirst().getId());
        assertEquals(this.secondExpense.getId().toString(), result.get(1).getId());
        assertEquals(zeroVatExpense.getId().toString(), result.get(2).getId());
    }

    @Test
    void shouldCountReceivedBook() {
        Expense excludedInvestmentAssetExpense = Expense.builder()
                .id(UUID.randomUUID())
                .series("2026")
                .number(4)
                .baseAmount(BigDecimal.valueOf(4000))
                .vatRate(21)
                .supplier(SupplierInfo.builder().name("Investment").identity("I10000000").build())
                .taxCategory(TaxCategory.OTROS)
                .depreciationRate(10)
                .issueDate(LocalDate.of(2026, 3, 19))
                .withholdingTax(BigDecimal.ZERO)
                .build();
        this.expenseRepository.save(new ExpenseEntity(excludedInvestmentAssetExpense));

        long count = this.expenseRepository.countReceivedBook(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), INVESTMENT_ASSET_THRESHOLD);

        assertEquals(3, count);
    }

    @Test
    void shouldFindReceivedInvestmentBook() {
        Expense investmentAssetExpense = Expense.builder()
                .id(UUID.randomUUID())
                .series("2026")
                .number(4)
                .baseAmount(BigDecimal.valueOf(4000))
                .vatRate(21)
                .supplier(SupplierInfo.builder().name("Investment").identity("I10000000").build())
                .taxCategory(TaxCategory.OTROS)
                .depreciationRate(10)
                .issueDate(LocalDate.of(2026, 3, 19))
                .withholdingTax(BigDecimal.ZERO)
                .build();
        this.expenseRepository.save(new ExpenseEntity(investmentAssetExpense));

        List<ExpenseEntity> result = this.expenseRepository.findReceivedInvestmentBook(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), INVESTMENT_ASSET_THRESHOLD);

        assertEquals(1, result.size());
        assertEquals(investmentAssetExpense.getId().toString(), result.getFirst().getId());
    }

    @Test
    void shouldFindReceivedInvestmentBookByNumberRange() {
        Expense investmentAssetExpense = Expense.builder()
                .id(UUID.randomUUID())
                .series("2026")
                .number(4)
                .baseAmount(BigDecimal.valueOf(4000))
                .vatRate(21)
                .supplier(SupplierInfo.builder().name("Investment").identity("I10000000").build())
                .taxCategory(TaxCategory.OTROS)
                .depreciationRate(10)
                .issueDate(LocalDate.of(2026, 3, 19))
                .withholdingTax(BigDecimal.ZERO)
                .build();
        Expense zeroVatInvestmentAssetExpense = Expense.builder()
                .id(UUID.randomUUID())
                .series("2026")
                .number(5)
                .baseAmount(BigDecimal.valueOf(4000))
                .vatRate(0)
                .supplier(SupplierInfo.builder().name("No Vat Investment").identity("I20000000").build())
                .taxCategory(TaxCategory.OTROS)
                .depreciationRate(10)
                .issueDate(LocalDate.of(2026, 3, 20))
                .withholdingTax(BigDecimal.ZERO)
                .build();
        Expense smallInvestmentAssetExpense = Expense.builder()
                .id(UUID.randomUUID())
                .series("2026")
                .number(6)
                .baseAmount(BigDecimal.valueOf(300))
                .vatRate(21)
                .supplier(SupplierInfo.builder().name("Small Investment").identity("I30000000").build())
                .taxCategory(TaxCategory.OTROS)
                .depreciationRate(10)
                .issueDate(LocalDate.of(2026, 3, 21))
                .withholdingTax(BigDecimal.ZERO)
                .build();
        this.expenseRepository.saveAll(List.of(
                new ExpenseEntity(investmentAssetExpense),
                new ExpenseEntity(zeroVatInvestmentAssetExpense),
                new ExpenseEntity(smallInvestmentAssetExpense)
        ));

        List<ExpenseEntity> result = this.expenseRepository.findReceivedInvestmentBook(
                "2026", 4, 6, INVESTMENT_ASSET_THRESHOLD);

        assertEquals(1, result.size());
        assertEquals(investmentAssetExpense.getId().toString(), result.getFirst().getId());
    }

    @Test
    void shouldFindInvestmentAssetsUntilByNumber() {
        Expense investmentAssetExpense = Expense.builder()
                .id(UUID.randomUUID())
                .series("2026")
                .number(4)
                .baseAmount(BigDecimal.valueOf(4000))
                .vatRate(21)
                .supplier(SupplierInfo.builder().name("Investment").identity("I10000000").build())
                .taxCategory(TaxCategory.OTROS)
                .depreciationRate(10)
                .issueDate(LocalDate.of(2026, 3, 19))
                .withholdingTax(BigDecimal.ZERO)
                .build();
        Expense laterInvestmentAssetExpense = Expense.builder()
                .id(UUID.randomUUID())
                .series("2026")
                .number(5)
                .baseAmount(BigDecimal.valueOf(5000))
                .vatRate(21)
                .supplier(SupplierInfo.builder().name("Later Investment").identity("I20000000").build())
                .taxCategory(TaxCategory.OTROS)
                .depreciationRate(10)
                .issueDate(LocalDate.of(2026, 3, 20))
                .withholdingTax(BigDecimal.ZERO)
                .build();
        this.expenseRepository.saveAll(List.of(
                new ExpenseEntity(investmentAssetExpense),
                new ExpenseEntity(laterInvestmentAssetExpense)
        ));

        List<ExpenseEntity> result = this.expenseRepository.findInvestmentAssetsUntil("2026", 4);

        assertEquals(1, result.size());
        assertEquals(investmentAssetExpense.getId().toString(), result.getFirst().getId());
    }

}
