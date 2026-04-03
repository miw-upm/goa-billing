package es.upm.api.infrastructure.mongodb.persistence;

import es.upm.api.domain.exceptions.NotFoundException;
import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.ExpenseFindCriteria;
import es.upm.api.infrastructure.mongodb.entities.ExpenseEntity;
import es.upm.api.infrastructure.mongodb.repositories.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class ExpensePersistenceMongodbIT {

    @Autowired
    private ExpensePersistenceMongodb expensePersistenceMongodb;

    @MockitoBean
    private ExpenseRepository expenseRepository;

    private Expense expense;
    private final ExpenseFindCriteria criteria = new ExpenseFindCriteria();
    private final UUID ENGAGEMENT_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeee000000");
    private final LocalDate DATE = LocalDate.of(2026, 3, 20);

    @BeforeEach
    void setUp() {
        this.expense = Expense.builder()
                .id(UUID.randomUUID())
                .engagementId(ENGAGEMENT_ID)
                .amount(BigDecimal.valueOf(25))
                .date(DATE)
                .description("Taxi")
                .build();
    }

    @Test
    void shouldCreateExpense() {
        when(this.expenseRepository.save(any(ExpenseEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        this.expensePersistenceMongodb.create(this.expense);

        ArgumentCaptor<ExpenseEntity> expenseEntityCaptor = ArgumentCaptor.forClass(ExpenseEntity.class);
        verify(this.expenseRepository).save(expenseEntityCaptor.capture());

        ExpenseEntity persistedExpenseEntity = expenseEntityCaptor.getValue();
        assertEquals(this.expense.getId(), persistedExpenseEntity.getId());
        assertEquals(this.expense.getEngagementId(), persistedExpenseEntity.getEngagementId());
        assertEquals(this.expense.getAmount(), persistedExpenseEntity.getAmount());
        assertEquals(this.expense.getDate(), persistedExpenseEntity.getDate());
        assertEquals(this.expense.getDescription(), persistedExpenseEntity.getDescription());
    }

    @Test
    void shouldPropagateExceptionWhenRepositoryFails() {
        RuntimeException exception = new RuntimeException("Mongo error");
        when(this.expenseRepository.save(any(ExpenseEntity.class))).thenThrow(exception);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> this.expensePersistenceMongodb.create(this.expense));

        assertEquals("Mongo error", thrown.getMessage());
        verify(this.expenseRepository).save(any(ExpenseEntity.class));
    }

    @Test
    void shouldReadExpenseById() {
        when(this.expenseRepository.findById(this.expense.getId()))
                .thenReturn(Optional.of(new ExpenseEntity(this.expense)));

        Expense readExpense = this.expensePersistenceMongodb.readById(this.expense.getId());

        assertEquals(this.expense, readExpense);
        verify(this.expenseRepository).findById(this.expense.getId());
    }

    @Test
    void shouldThrowNotFoundExceptionWhenExpenseDoesNotExist() {
        when(this.expenseRepository.findById(this.expense.getId()))
                .thenReturn(Optional.empty());

        NotFoundException thrown = assertThrows(NotFoundException.class,
                () -> this.expensePersistenceMongodb.readById(this.expense.getId()));

        assertEquals("Not Found Exception. Expense id: " + this.expense.getId(), thrown.getMessage());
        verify(this.expenseRepository).findById(this.expense.getId());
    }

    @Test
    void shouldFindAll() {
        when(this.expenseRepository.findAll(ExpensePersistenceMongodb.DATE))
                .thenReturn(List.of(new ExpenseEntity(this.expense)));

        Stream<Expense> expenseStream = this.expensePersistenceMongodb.findAll(this.criteria);

        verify(this.expenseRepository).findAll(ExpensePersistenceMongodb.DATE);

        assertEquals(this.expense, expenseStream.findFirst().orElse(null));
    }

    @Test
    void shouldFindAllWithDate() {
        this.criteria.setDate(DATE);
        when(this.expenseRepository.findByDate(DATE))
                .thenReturn(List.of(new ExpenseEntity(this.expense)));

        Stream<Expense> expenseStream = this.expensePersistenceMongodb.findAll(this.criteria);

        verify(this.expenseRepository).findByDate(DATE);

        assertEquals(this.expense, expenseStream.findFirst().orElse(null));
    }

    @Test
    void shouldFindAllWithEngagementId() {
        this.criteria.setEngagementId(ENGAGEMENT_ID);
        when(this.expenseRepository.findByEngagementId(ENGAGEMENT_ID))
                .thenReturn(List.of(new ExpenseEntity(this.expense)));

        Stream<Expense> expenseStream = this.expensePersistenceMongodb.findAll(this.criteria);

        verify(this.expenseRepository).findByEngagementId(ENGAGEMENT_ID);

        assertEquals(this.expense, expenseStream.findFirst().orElse(null));
    }

    @Test
    void shouldFindAllWithEngagementIdAndDate() {
        this.criteria.setEngagementId(ENGAGEMENT_ID);
        this.criteria.setDate(DATE);
        when(this.expenseRepository.findByEngagementId(ENGAGEMENT_ID))
                .thenReturn(List.of(new ExpenseEntity(this.expense)));

        Stream<Expense> expenseStream = this.expensePersistenceMongodb.findAll(this.criteria);

        verify(this.expenseRepository).findByEngagementId(ENGAGEMENT_ID);

        assertEquals(this.expense, expenseStream.findFirst().orElse(null));
    }
}
