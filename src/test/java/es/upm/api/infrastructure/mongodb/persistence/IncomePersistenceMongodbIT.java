package es.upm.api.infrastructure.mongodb.persistence;

import es.upm.api.domain.model.Income;
import es.upm.api.infrastructure.mongodb.entities.IncomeEntity;
import es.upm.api.infrastructure.mongodb.repositories.IncomeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class IncomePersistenceMongodbIT {

    @Autowired
    private IncomePersistenceMongodb incomePersistenceMongodb;

    @MockitoBean
    private IncomeRepository incomeRepository;

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
    void shouldCreateIncome() {
        when(this.incomeRepository.save(any(IncomeEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        this.incomePersistenceMongodb.create(this.income);

        ArgumentCaptor<IncomeEntity> incomeEntityCaptor = ArgumentCaptor.forClass(IncomeEntity.class);
        verify(this.incomeRepository).save(incomeEntityCaptor.capture());

        IncomeEntity persistedIncomeEntity = incomeEntityCaptor.getValue();
        assertEquals(this.income.getId(), persistedIncomeEntity.getId());
        assertEquals(this.income.getEngagementId(), persistedIncomeEntity.getEngagementId());
        assertEquals(this.income.getUserId(), persistedIncomeEntity.getUserId());
        assertEquals(this.income.getAmount(), persistedIncomeEntity.getAmount());
        assertEquals(this.income.getDate(), persistedIncomeEntity.getDate());
    }

    @Test
    void shouldPropagateExceptionWhenRepositoryFails() {
        RuntimeException exception = new RuntimeException("Mongo error");
        when(this.incomeRepository.save(any(IncomeEntity.class))).thenThrow(exception);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> this.incomePersistenceMongodb.create(this.income));

        assertEquals("Mongo error", thrown.getMessage());
        verify(this.incomeRepository).save(any(IncomeEntity.class));
    }

    @Test
    void shouldFindAllIncomes() {
        IncomeEntity newestIncome = new IncomeEntity(Income.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(300))
                .date(LocalDate.of(2026, 3, 21))
                .build());

        IncomeEntity oldestIncome = new IncomeEntity(Income.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(150))
                .date(LocalDate.of(2026, 3, 20))
                .build());

        when(this.incomeRepository.findAll(Sort.by(Sort.Direction.DESC, "date")))
                .thenReturn(List.of(newestIncome, oldestIncome));

        List<Income> incomes = this.incomePersistenceMongodb.findAll().toList();

        assertEquals(2, incomes.size());
        assertEquals(newestIncome.toIncome(), incomes.get(0));
        assertEquals(oldestIncome.toIncome(), incomes.get(1));
    }

    @Test
    void shouldReturnEmptyWhenNoIncomesExist() {
        when(this.incomeRepository.findAll(Sort.by(Sort.Direction.DESC, "date")))
                .thenReturn(List.of());

        List<Income> incomes = this.incomePersistenceMongodb.findAll().toList();

        assertEquals(0, incomes.size());
    }

    @Test
    void shouldFindIncomesByEngagementId() {
        UUID engagementId = UUID.randomUUID();

        IncomeEntity newestIncome = new IncomeEntity(Income.builder()
                .id(UUID.randomUUID())
                .engagementId(engagementId)
                .userId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(300))
                .date(LocalDate.of(2026, 3, 21))
                .build());

        IncomeEntity oldestIncome = new IncomeEntity(Income.builder()
                .id(UUID.randomUUID())
                .engagementId(engagementId)
                .userId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(150))
                .date(LocalDate.of(2026, 3, 20))
                .build());

        when(this.incomeRepository.findByEngagementId(engagementId, Sort.by(Sort.Direction.DESC, "date")))
                .thenReturn(List.of(newestIncome, oldestIncome));

        List<Income> incomes = this.incomePersistenceMongodb.findByEngagementId(engagementId).toList();

        assertEquals(2, incomes.size());
        assertEquals(newestIncome.toIncome(), incomes.get(0));
        assertEquals(oldestIncome.toIncome(), incomes.get(1));
    }
}