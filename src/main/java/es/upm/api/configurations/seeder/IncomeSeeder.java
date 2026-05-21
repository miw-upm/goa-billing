package es.upm.api.configurations.seeder;

import es.upm.api.adapter.out.billing.mongo.income.IncomeEntity;
import es.upm.api.adapter.out.billing.mongo.income.IncomeRepository;
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
public class IncomeSeeder {

    private final IncomeRepository incomeRepository;

    public IncomeSeeder(IncomeRepository incomeRepository) {
        this.incomeRepository = incomeRepository;
    }

    public void seedDatabase() {
        log.warn("------- Income Initial Load -----------");
        List<IncomeEntity> incomes = List.of(
                this.buildIncome(
                        "bbbbbbbb-cccc-dddd-eeee-ffffaaaab001",
                        "aaaaaaa0-bbbb-cccc-dddd-eeeeffff0000",
                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0000",
                        "500.00",
                        LocalDate.of(2026, 3, 20)
                ),
                this.buildIncome(
                        "bbbbbbbb-cccc-dddd-eeee-ffffaaaab002",
                        "aaaaaaa0-bbbb-cccc-dddd-eeeeffff0001",
                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0001",
                        "1200.00",
                        LocalDate.of(2026, 3, 21)
                ),
                this.buildIncome(
                        "bbbbbbbb-cccc-dddd-eeee-ffffaaaab003",
                        "aaaaaaa0-bbbb-cccc-dddd-eeeeffff0001",
                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0000",
                        "200.00",
                        LocalDate.of(2026, 3, 22)
                ),
                this.buildIncome(
                        "bbbbbbbb-cccc-dddd-eeee-ffffaaaab004",
                        "aaaaaaa0-bbbb-cccc-dddd-eeeeffff0001",
                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0001",
                        "875.00",
                        LocalDate.of(2026, 3, 23)
                ),
                this.buildIncome(
                        "bbbbbbbb-cccc-dddd-eeee-ffffaaaab005",
                        "aaaaaaa0-bbbb-cccc-dddd-eeeeffff0001",
                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0000",
                        "325.00",
                        LocalDate.of(2026, 3, 24)
                )
        );
        this.incomeRepository.saveAll(incomes);
    }

    public void deleteAll() {
        this.incomeRepository.deleteAll();
    }

    private IncomeEntity buildIncome(String id, String engagementId, String userId, String amount, LocalDate date) {
        IncomeEntity incomeEntity = new IncomeEntity();
        incomeEntity.setId(UUID.fromString(id));
        incomeEntity.setEngagementId(UUID.fromString(engagementId));
        incomeEntity.setUserId(UUID.fromString(userId));
        incomeEntity.setAmount(new BigDecimal(amount));
        incomeEntity.setDate(date);
        return incomeEntity;
    }
}
