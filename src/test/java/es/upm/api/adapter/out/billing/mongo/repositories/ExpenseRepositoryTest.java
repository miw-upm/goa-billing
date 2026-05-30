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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataMongoTest
@ActiveProfiles("test")
class ExpenseRepositoryTest {

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
    void shouldFindBySupplierContainsOrderByIssueDateDesc() {
        List<ExpenseEntity> result = this.expenseRepository
                .findBySupplierNameContainingIgnoreCaseOrSupplierIdentityContainingIgnoreCaseOrderByIssueDateDesc(
                        "taxi", "taxi");

        assertEquals(2, result.size());
        assertEquals(this.secondExpense.getId().toString(), result.getFirst().getId());
        assertEquals(this.firstExpense.getId().toString(), result.get(1).getId());
    }

    @Test
    void shouldFindByIssueDateGreaterThanEqualOrderByIssueDateDesc() {
        List<ExpenseEntity> result = this.expenseRepository
                .findByIssueDateGreaterThanEqualOrderByIssueDateDesc(LocalDate.of(2026, 3, 20));

        assertEquals(2, result.size());
        assertEquals(this.secondExpense.getId().toString(), result.getFirst().getId());
        assertEquals(this.firstExpense.getId().toString(), result.get(1).getId());
    }

    @Test
    void shouldFindByEngagementIdPrefixOrderByIssueDateDesc() {
        String prefix = this.firstExpense.getEngagement().getId().toString().substring(0, 4);
        List<ExpenseEntity> result = this.expenseRepository.findByEngagementIdStartingWithOrderByIssueDateDesc(prefix);

        assertEquals(2, result.size());
        assertEquals(this.secondExpense.getId().toString(), result.getFirst().getId());
        assertEquals(this.firstExpense.getId().toString(), result.get(1).getId());
    }

}
