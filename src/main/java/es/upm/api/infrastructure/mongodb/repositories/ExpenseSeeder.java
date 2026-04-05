package es.upm.api.infrastructure.mongodb.repositories;

import es.upm.api.infrastructure.mongodb.entities.ExpenseEntity;
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
                        "aaaaaaa0-bbbb-cccc-dddd-eeeeffff0000",
                        "35.50",
                        LocalDate.of(2026, 3, 15),
                        "Taxi from airport"
                ),
                this.buildExpense(
                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff1001",
                        "aaaaaaa0-bbbb-cccc-dddd-eeeeffff0001",
                        "120.00",
                        LocalDate.of(2026, 3, 16),
                        "Hotel accommodation"
                ),
                this.buildExpense(
                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff1002",
                        "aaaaaaa0-bbbb-cccc-dddd-eeeeffff0000",
                        "18.90",
                        LocalDate.of(2026, 3, 17),
                        "Team lunch"
                ),
                this.buildExpense(
                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff1004",
                        "aaaaaaa0-bbbb-cccc-dddd-eeeeffff0001",
                        "64.80",
                        LocalDate.of(2026, 3, 18),
                        "Client dinner"
                )
        );
        this.expenseRepository.saveAll(expenses);
    }

    public void deleteAll() {
        this.expenseRepository.deleteAll();
    }

    private ExpenseEntity buildExpense(String id, String engagementId, String amount, LocalDate date, String description) {
        ExpenseEntity expenseEntity = new ExpenseEntity();
        expenseEntity.setId(UUID.fromString(id));
        expenseEntity.setEngagementId(UUID.fromString(engagementId));
        expenseEntity.setAmount(new BigDecimal(amount));
        expenseEntity.setDate(date);
        expenseEntity.setDescription(description);
        return expenseEntity;
    }
}
