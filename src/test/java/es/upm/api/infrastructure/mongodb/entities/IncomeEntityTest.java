package es.upm.api.infrastructure.mongodb.entities;

import es.upm.api.domain.model.Income;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IncomeEntityTest {

    private Income income;

    @BeforeEach
    void setUp() {
        this.income = Income.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(250))
                .date(LocalDate.of(2026, 3, 20))
                .build();
    }

    @Test
    void shouldBuildIncomeEntityFromIncome() {
        IncomeEntity incomeEntity = new IncomeEntity(this.income);

        assertEquals(this.income.getId(), incomeEntity.getId());
        assertEquals(this.income.getEngagementId(), incomeEntity.getEngagementId());
        assertEquals(this.income.getUserId(), incomeEntity.getUserId());
        assertEquals(this.income.getAmount(), incomeEntity.getAmount());
        assertEquals(this.income.getDate(), incomeEntity.getDate());
    }

    @Test
    void shouldConvertIncomeEntityToIncome() {
        IncomeEntity incomeEntity = new IncomeEntity();
        incomeEntity.setId(this.income.getId());
        incomeEntity.setEngagementId(this.income.getEngagementId());
        incomeEntity.setUserId(this.income.getUserId());
        incomeEntity.setAmount(this.income.getAmount());
        incomeEntity.setDate(this.income.getDate());

        Income mappedIncome = incomeEntity.toIncome();

        assertEquals(incomeEntity.getId(), mappedIncome.getId());
        assertEquals(incomeEntity.getEngagementId(), mappedIncome.getEngagementId());
        assertEquals(incomeEntity.getUserId(), mappedIncome.getUserId());
        assertEquals(incomeEntity.getAmount(), mappedIncome.getAmount());
        assertEquals(incomeEntity.getDate(), mappedIncome.getDate());
    }
}