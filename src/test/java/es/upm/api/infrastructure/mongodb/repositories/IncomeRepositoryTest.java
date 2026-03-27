package es.upm.api.infrastructure.mongodb.repositories;

import es.upm.api.domain.model.Income;
import es.upm.api.infrastructure.mongodb.entities.IncomeEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@ActiveProfiles("test")
class IncomeRepositoryTest {

    @Autowired
    private IncomeRepository incomeRepository;

    private Income income;

    @BeforeEach
    void setUp() {
        this.incomeRepository.deleteAll();
        this.income = Income.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(300))
                .date(LocalDate.of(2026, 3, 20))
                .build();
    }

    @Test
    void shouldSaveIncome() {
        IncomeEntity incomeEntity = new IncomeEntity(this.income);

        IncomeEntity savedIncomeEntity = this.incomeRepository.save(incomeEntity);

        assertNotNull(savedIncomeEntity);
        assertNotNull(savedIncomeEntity.getId());
        assertEquals(this.income.getId(), savedIncomeEntity.getId());
        assertEquals(this.income.getEngagementId(), savedIncomeEntity.getEngagementId());
        assertEquals(this.income.getUserId(), savedIncomeEntity.getUserId());
        assertEquals(this.income.getAmount(), savedIncomeEntity.getAmount());
        assertEquals(this.income.getDate(), savedIncomeEntity.getDate());
    }

    @Test
    void shouldFindIncomeById() {
        IncomeEntity savedIncomeEntity = this.incomeRepository.save(new IncomeEntity(this.income));

        Optional<IncomeEntity> optionalIncomeEntity = this.incomeRepository.findById(savedIncomeEntity.getId());

        assertTrue(optionalIncomeEntity.isPresent());
        IncomeEntity foundIncomeEntity = optionalIncomeEntity.get();
        assertEquals(savedIncomeEntity.getId(), foundIncomeEntity.getId());
        assertEquals(savedIncomeEntity.getEngagementId(), foundIncomeEntity.getEngagementId());
        assertEquals(savedIncomeEntity.getUserId(), foundIncomeEntity.getUserId());
        assertEquals(savedIncomeEntity.getAmount(), foundIncomeEntity.getAmount());
        assertEquals(savedIncomeEntity.getDate(), foundIncomeEntity.getDate());
    }

    @Test
    void shouldFindAllIncomesOrderedByDateDesc() {
        IncomeEntity oldestIncome = this.incomeRepository.save(new IncomeEntity(Income.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(100))
                .date(LocalDate.of(2026, 3, 20))
                .build()));

        IncomeEntity newestIncome = this.incomeRepository.save(new IncomeEntity(Income.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(300))
                .date(LocalDate.of(2026, 3, 21))
                .build()));

        List<IncomeEntity> incomes = this.incomeRepository.findAll(Sort.by(Sort.Direction.DESC, "date"));

        assertFalse(incomes.isEmpty());
        assertEquals(newestIncome.getId(), incomes.get(0).getId());
        assertEquals(oldestIncome.getId(), incomes.get(1).getId());
    }

    @Test
    void shouldFindIncomesByEngagementIdOrderedByDateDesc() {
        UUID engagementId = UUID.randomUUID();

        IncomeEntity oldestIncome = this.incomeRepository.save(new IncomeEntity(Income.builder()
                .id(UUID.randomUUID())
                .engagementId(engagementId)
                .userId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(100))
                .date(LocalDate.of(2026, 3, 20))
                .build()));

        IncomeEntity newestIncome = this.incomeRepository.save(new IncomeEntity(Income.builder()
                .id(UUID.randomUUID())
                .engagementId(engagementId)
                .userId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(300))
                .date(LocalDate.of(2026, 3, 21))
                .build()));

        this.incomeRepository.save(new IncomeEntity(Income.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(999))
                .date(LocalDate.of(2026, 3, 22))
                .build()));

        List<IncomeEntity> incomes = this.incomeRepository.findByEngagementId(
                engagementId,
                Sort.by(Sort.Direction.DESC, "date")
        );

        assertEquals(2, incomes.size());
        assertEquals(newestIncome.getId(), incomes.get(0).getId());
        assertEquals(oldestIncome.getId(), incomes.get(1).getId());
    }
}