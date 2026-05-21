package es.upm.api.adapter.out.billing.mongo.persistence;

import es.upm.api.adapter.out.billing.mongo.income.IncomeAdapter;
import es.upm.api.domain.model.Income;
import es.upm.api.domain.model.criteria.IncomeFindCriteria;
import es.upm.api.adapter.out.billing.mongo.income.IncomeEntity;
import es.upm.api.adapter.out.billing.mongo.income.IncomeRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class IncomeAdapterIT {

    @Autowired
    private IncomeAdapter incomePersistenceMongodb;

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
        IncomeEntity firstIncome = new IncomeEntity(Income.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(300))
                .date(LocalDate.of(2026, 3, 20))
                .build());

        IncomeEntity secondIncome = new IncomeEntity(Income.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(150))
                .date(LocalDate.of(2026, 3, 19))
                .build());

        IncomeFindCriteria criteria = new IncomeFindCriteria(null, null);

        when(this.incomeRepository.findAll(Sort.by(Sort.Direction.DESC, "date")))
                .thenReturn(List.of(firstIncome, secondIncome));

        List<Income> incomes = this.incomePersistenceMongodb.findAll(criteria).toList();

        assertEquals(2, incomes.size());
        assertEquals(firstIncome.toDomain(), incomes.get(0));
        assertEquals(secondIncome.toDomain(), incomes.get(1));
        verify(this.incomeRepository).findAll(Sort.by(Sort.Direction.DESC, "date"));
    }

    @Test
    void shouldReturnEmptyWhenNoIncomesExist() {
        IncomeFindCriteria criteria = new IncomeFindCriteria(null, null);

        when(this.incomeRepository.findAll(any(Sort.class)))
                .thenReturn(List.of());

        List<Income> incomes = this.incomePersistenceMongodb.findAll(criteria).toList();

        assertTrue(incomes.isEmpty());
        verify(this.incomeRepository).findAll(any(Sort.class));
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
        assertEquals(newestIncome.toDomain(), incomes.get(0));
        assertEquals(oldestIncome.toDomain(), incomes.get(1));
    }

    @Test
    void shouldFindIncomesByDate() {
        LocalDate date = LocalDate.of(2026, 3, 20);

        IncomeEntity firstIncome = new IncomeEntity(Income.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(300))
                .date(date)
                .build());

        IncomeEntity secondIncome = new IncomeEntity(Income.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(150))
                .date(date)
                .build());

        IncomeFindCriteria criteria = new IncomeFindCriteria(null, date);

        when(this.incomeRepository.findByDate(date, Sort.by(Sort.Direction.DESC, "date")))
                .thenReturn(List.of(firstIncome, secondIncome));

        List<Income> incomes = this.incomePersistenceMongodb.findAll(criteria).toList();

        assertEquals(2, incomes.size());
        assertEquals(firstIncome.toDomain(), incomes.get(0));
        assertEquals(secondIncome.toDomain(), incomes.get(1));
    }

    @Test
    void shouldFindIncomesByEngagementIdAndDate() {
        UUID engagementId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 3, 20);

        IncomeEntity firstIncome = new IncomeEntity(Income.builder()
                .id(UUID.randomUUID())
                .engagementId(engagementId)
                .userId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(300))
                .date(date)
                .build());

        IncomeEntity secondIncome = new IncomeEntity(Income.builder()
                .id(UUID.randomUUID())
                .engagementId(engagementId)
                .userId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(150))
                .date(date)
                .build());

        IncomeFindCriteria criteria = new IncomeFindCriteria(engagementId, date);

        when(this.incomeRepository.findByEngagementIdAndDate(
                engagementId,
                date,
                Sort.by(Sort.Direction.DESC, "date")
        )).thenReturn(List.of(firstIncome, secondIncome));

        List<Income> incomes = this.incomePersistenceMongodb.findAll(criteria).toList();

        assertEquals(2, incomes.size());
        assertEquals(firstIncome.toDomain(), incomes.get(0));
        assertEquals(secondIncome.toDomain(), incomes.get(1));
    }
}