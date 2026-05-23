package es.upm.api.adapter.out.billing.mongo.persistence;

import es.upm.api.adapter.out.billing.mongo.expense.ExpenseAdapter;
import es.upm.api.adapter.out.billing.mongo.expense.ExpenseEntity;
import es.upm.api.adapter.out.billing.mongo.expense.ExpenseRepository;
import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.SupplierInfo;
import es.upm.api.domain.model.TaxCategory;
import es.upm.api.domain.model.criteria.ExpenseFindCriteria;
import es.upm.api.domain.model.external.EngagementSnapshot;
import es.upm.miw.exception.NotFoundException;
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
class ExpenseAdapterIT {

    @Autowired
    private ExpenseAdapter expensePersistenceMongodb;

    @MockitoBean
    private ExpenseRepository expenseRepository;

    private Expense expense;
    private final ExpenseFindCriteria criteria = new ExpenseFindCriteria();
    private final UUID engagementUuid = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeee000000");
    private final LocalDate date = LocalDate.of(2026, 3, 20);

    @BeforeEach
    void setUp() {
        this.expense = Expense.builder()
                .id(UUID.randomUUID())
                .engagement(EngagementSnapshot.builder().id(engagementUuid).build())
                .baseAmount(BigDecimal.valueOf(25))
                .vatRate(21)
                .supplier(SupplierInfo.builder().name("Taxi Madrid").identity("A10000000").build())
                .taxCategory(TaxCategory.OTROS)
                .issueDate(date)
                .withholdingTax(BigDecimal.ZERO)
                .documentPath("doc/path")
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
        assertEquals(this.expense.getEngagement().getId(), persistedExpenseEntity.getEngagementId());
        assertEquals(this.expense.getBaseAmount(), persistedExpenseEntity.getBaseAmount());
        assertEquals(this.expense.getVatRate(), persistedExpenseEntity.getVatRate());
        assertEquals(this.expense.getSupplier(), persistedExpenseEntity.getSupplier());
        assertEquals(this.expense.getTaxCategory(), persistedExpenseEntity.getTaxCategory());
        assertEquals(this.expense.getIssueDate(), persistedExpenseEntity.getIssueDate());
        assertEquals(this.expense.getDocumentPath(), persistedExpenseEntity.getDocumentPath());
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

        Expense readExpense = this.expensePersistenceMongodb.read(this.expense.getId());

        assertEquals(this.expense, readExpense);
        verify(this.expenseRepository).findById(this.expense.getId());
    }

    @Test
    void shouldThrowNotFoundExceptionWhenExpenseDoesNotExist() {
        when(this.expenseRepository.findById(this.expense.getId()))
                .thenReturn(Optional.empty());

        NotFoundException thrown = assertThrows(NotFoundException.class,
                () -> this.expensePersistenceMongodb.read(this.expense.getId()));

        assertEquals("Not Found Exception. Expense id: " + this.expense.getId(), thrown.getMessage());
        verify(this.expenseRepository).findById(this.expense.getId());
    }

       @Test
    void shouldDeleteExpense() {
        UUID id = this.expense.getId();
        ExpenseEntity expenseEntity = new ExpenseEntity(this.expense);
        when(this.expenseRepository.findById(id)).thenReturn(Optional.of(expenseEntity));

        this.expensePersistenceMongodb.delete(id);

        verify(this.expenseRepository).findById(id);
        verify(this.expenseRepository).delete(expenseEntity);
    }

    @Test
    void shouldThrowNotFoundWhenDeleteMissing() {
        UUID id = this.expense.getId();
        when(this.expenseRepository.findById(id)).thenReturn(Optional.empty());

        NotFoundException thrown = assertThrows(NotFoundException.class,
                () -> this.expensePersistenceMongodb.delete(id));

        assertEquals("Not Found Exception. Expense id: " + id, thrown.getMessage());
        verify(this.expenseRepository).findById(id);
    }

    @Test
    void shouldFindWithDate() {
        this.criteria.setFromDate(date);
        when(this.expenseRepository.findByIssueDateGreaterThanEqualOrderByIssueDateDesc(date))
                .thenReturn(List.of(new ExpenseEntity(this.expense)));

        Stream<Expense> expenseStream = this.expensePersistenceMongodb.find(this.criteria);

        verify(this.expenseRepository).findByIssueDateGreaterThanEqualOrderByIssueDateDesc(date);

        assertEquals(this.expense, expenseStream.findFirst().orElse(null));
    }

    @Test
    void shouldFindWithEngagementId() {
        this.criteria.setEngagementId(engagementUuid);
        when(this.expenseRepository.findByEngagementIdOrderByIssueDateDesc(engagementUuid))
                .thenReturn(List.of(new ExpenseEntity(this.expense)));

        Stream<Expense> expenseStream = this.expensePersistenceMongodb.find(this.criteria);

        verify(this.expenseRepository).findByEngagementIdOrderByIssueDateDesc(engagementUuid);

        assertEquals(this.expense, expenseStream.findFirst().orElse(null));
    }

    @Test
    void shouldFindWithEngagementIdAndDate() {
        this.criteria.setEngagementId(engagementUuid);
        this.criteria.setFromDate(date);
        when(this.expenseRepository.findByEngagementIdAndIssueDateGreaterThanEqualOrderByIssueDateDesc(
                engagementUuid, date
        ))
                .thenReturn(List.of(new ExpenseEntity(this.expense)));

        Stream<Expense> expenseStream = this.expensePersistenceMongodb.find(this.criteria);

        verify(this.expenseRepository).findByEngagementIdAndIssueDateGreaterThanEqualOrderByIssueDateDesc(
                engagementUuid, date
        );

        assertEquals(this.expense, expenseStream.findFirst().orElse(null));
    }
}
