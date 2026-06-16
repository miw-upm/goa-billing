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
import org.bson.types.Decimal128;
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
    private static final BigDecimal INVESTMENT_ASSET_THRESHOLD = new BigDecimal("3005.06");
    private static final Decimal128 INVESTMENT_ASSET_THRESHOLD_DECIMAL = Decimal128.parse("3005.06");

    private final ExpenseFindCriteria criteria = new ExpenseFindCriteria();
    private final UUID engagementUuid = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeee000000");
    private final LocalDate date = LocalDate.of(2026, 3, 20);
    @Autowired
    private ExpenseAdapter expensePersistenceMongodb;
    @MockitoBean
    private ExpenseRepository expenseRepository;
    private Expense expense;

    @BeforeEach
    void setUp() {
        this.expense = Expense.builder()
                .id(UUID.randomUUID())
                .engagement(EngagementSnapshot.builder().id(engagementUuid).build())
                .baseAmount(BigDecimal.valueOf(25))
                .vatRate(21)
                .supplier(SupplierInfo.builder().name("Taxi Madrid").identity("A10000000").build())
                .taxCategory(TaxCategory.OTROS)
                .depreciationRate(100)
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
        assertEquals(this.expense.getId().toString(), persistedExpenseEntity.getId());
        assertEquals(this.expense.getEngagement().getId().toString(), persistedExpenseEntity.getEngagementId());
        assertEquals(this.expense.getBaseAmount(), persistedExpenseEntity.getBaseAmount());
        assertEquals(this.expense.getVatRate(), persistedExpenseEntity.getVatRate());
        assertEquals(this.expense.getSupplier(), persistedExpenseEntity.getSupplier().toDomain());
        assertEquals(this.expense.getTaxCategory(), persistedExpenseEntity.getTaxCategory());
        assertEquals(this.expense.getDepreciationRate(), persistedExpenseEntity.getDepreciationRate());
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
        when(this.expenseRepository.findById(this.expense.getId().toString()))
                .thenReturn(Optional.of(new ExpenseEntity(this.expense)));

        Expense readExpense = this.expensePersistenceMongodb.read(this.expense.getId());

        assertEquals(this.expense, readExpense);
        verify(this.expenseRepository).findById(this.expense.getId().toString());
    }

    @Test
    void shouldThrowNotFoundExceptionWhenExpenseDoesNotExist() {
        when(this.expenseRepository.findById(this.expense.getId().toString()))
                .thenReturn(Optional.empty());

        NotFoundException thrown = assertThrows(NotFoundException.class,
                () -> this.expensePersistenceMongodb.read(this.expense.getId()));

        assertEquals("Not Found Exception. Expense id: " + this.expense.getId(), thrown.getMessage());
        verify(this.expenseRepository).findById(this.expense.getId().toString());
    }

    @Test
    void shouldFindWithDate() {
        this.criteria.setFromDate(date);
        Expense oldExpense = Expense.builder()
                .id(UUID.randomUUID())
                .engagement(EngagementSnapshot.builder().id(engagementUuid).build())
                .baseAmount(BigDecimal.valueOf(15))
                .vatRate(21)
                .supplier(SupplierInfo.builder().name("Old supplier").identity("X100").build())
                .taxCategory(TaxCategory.OTROS)
                .depreciationRate(100)
                .issueDate(date.minusDays(1))
                .withholdingTax(BigDecimal.ZERO)
                .documentPath("doc/path")
                .build();
        when(this.expenseRepository.findAllByOrderBySeriesDescNumberDesc())
                .thenReturn(List.of(new ExpenseEntity(this.expense), new ExpenseEntity(oldExpense)));

        Stream<Expense> expenseStream = this.expensePersistenceMongodb.find(this.criteria);

        verify(this.expenseRepository).findAllByOrderBySeriesDescNumberDesc();

        assertEquals(1, expenseStream.toList().size());
    }

    @Test
    void shouldFindWithCategory() {
        this.criteria.setCategory("tro");
        Expense differentCategoryExpense = Expense.builder()
                .id(UUID.randomUUID())
                .engagement(EngagementSnapshot.builder().id(engagementUuid).build())
                .baseAmount(BigDecimal.valueOf(18))
                .vatRate(21)
                .supplier(SupplierInfo.builder().name("Power Co").identity("P200").build())
                .taxCategory(TaxCategory.SUMINISTROS)
                .depreciationRate(10)
                .issueDate(date)
                .withholdingTax(BigDecimal.ZERO)
                .documentPath("doc/path")
                .build();
        when(this.expenseRepository.findAllByOrderBySeriesDescNumberDesc())
                .thenReturn(List.of(new ExpenseEntity(this.expense), new ExpenseEntity(differentCategoryExpense)));

        Stream<Expense> expenseStream = this.expensePersistenceMongodb.find(this.criteria);

        verify(this.expenseRepository).findAllByOrderBySeriesDescNumberDesc();

        List<Expense> filtered = expenseStream.toList();
        assertEquals(2, filtered.size());
    }

    @Test
    void shouldFindWithSupplier() {
        this.criteria.setSupplier("100");
        when(this.expenseRepository.findBySupplierNameContainingIgnoreCaseOrSupplierIdentityContainingIgnoreCaseOrderBySeriesDescNumberDesc(
                "100", "100"
        ))
                .thenReturn(List.of(new ExpenseEntity(this.expense)));

        Stream<Expense> expenseStream = this.expensePersistenceMongodb.find(this.criteria);

        verify(this.expenseRepository).findBySupplierNameContainingIgnoreCaseOrSupplierIdentityContainingIgnoreCaseOrderBySeriesDescNumberDesc(
                "100", "100"
        );

        assertEquals(this.expense, expenseStream.findFirst().orElse(null));
    }

    @Test
    void shouldReturnEmptyWhenCategoryDoesNotMatch() {
        this.criteria.setCategory("DOES_NOT_EXIST");
        when(this.expenseRepository.findAllByOrderBySeriesDescNumberDesc())
                .thenReturn(List.of(new ExpenseEntity(this.expense)));

        assertEquals(0, this.expensePersistenceMongodb.find(this.criteria).toList().size());
    }

    @Test
    void shouldFindSuppliersByNameOrIdentityContains() {
        SupplierInfo supplierInfo = SupplierInfo.builder()
                .name("Taxi Madrid")
                .identity("A10000000")
                .build();
        Expense first = Expense.builder()
                .id(UUID.randomUUID())
                .engagement(EngagementSnapshot.builder().id(this.engagementUuid).build())
                .baseAmount(BigDecimal.valueOf(25))
                .vatRate(21)
                .supplier(supplierInfo)
                .taxCategory(TaxCategory.OTROS)
                .depreciationRate(100)
                .issueDate(this.date)
                .withholdingTax(BigDecimal.ZERO)
                .documentPath("doc/path")
                .build();
        Expense second = Expense.builder()
                .id(UUID.randomUUID())
                .engagement(EngagementSnapshot.builder().id(this.engagementUuid).build())
                .baseAmount(BigDecimal.valueOf(30))
                .vatRate(21)
                .supplier(supplierInfo)
                .taxCategory(TaxCategory.OTROS)
                .depreciationRate(100)
                .issueDate(this.date.plusDays(1))
                .withholdingTax(BigDecimal.ZERO)
                .documentPath("doc/path")
                .build();

        when(this.expenseRepository.findBySupplierNameContainingIgnoreCaseOrSupplierIdentityContainingIgnoreCase("100", "100"))
                .thenReturn(List.of(new ExpenseEntity(first), new ExpenseEntity(second)));

        List<SupplierInfo> suppliers = this.expensePersistenceMongodb.findSuppliers("100").toList();

        assertEquals(1, suppliers.size());
        assertEquals("Taxi Madrid", suppliers.get(0).getName());
        assertEquals("A10000000", suppliers.get(0).getIdentity());
        verify(this.expenseRepository).findBySupplierNameContainingIgnoreCaseOrSupplierIdentityContainingIgnoreCase("100", "100");
    }

    @Test
    void shouldFindByEngagementId() {
        when(this.expenseRepository.findByEngagementIdOrderByIssueDateDesc(this.engagementUuid.toString()))
                .thenReturn(List.of(new ExpenseEntity(this.expense)));

        List<Expense> expenses = this.expensePersistenceMongodb.findByEngagementId(this.engagementUuid).toList();

        assertEquals(1, expenses.size());
        assertEquals(this.expense.getId(), expenses.getFirst().getId());
        verify(this.expenseRepository).findByEngagementIdOrderByIssueDateDesc(this.engagementUuid.toString());
    }

    @Test
    void shouldFindByEngagementIdPrefix() {
        String engagementIdPrefix = this.engagementUuid.toString().substring(0, 4);
        this.criteria.setEngagementId(engagementIdPrefix);
        when(this.expenseRepository.findByEngagementIdStartingWithOrderBySeriesDescNumberDesc(engagementIdPrefix))
                .thenReturn(List.of(new ExpenseEntity(this.expense)));

        List<Expense> expenses = this.expensePersistenceMongodb.find(this.criteria).toList();

        assertEquals(1, expenses.size());
        assertEquals(this.expense.getId(), expenses.getFirst().getId());
        verify(this.expenseRepository).findByEngagementIdStartingWithOrderBySeriesDescNumberDesc(engagementIdPrefix);
    }

    @Test
    void shouldFindInvoiceReceivedBook() {
        LocalDate fromDate = LocalDate.of(2026, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 6, 30);
        when(this.expenseRepository.findReceivedBook(fromDate, toDate, INVESTMENT_ASSET_THRESHOLD_DECIMAL))
                .thenReturn(List.of(new ExpenseEntity(this.expense)));

        List<Expense> expenses = this.expensePersistenceMongodb
                .findInvoiceReceivedBook(fromDate, toDate, INVESTMENT_ASSET_THRESHOLD).toList();

        assertEquals(List.of(this.expense), expenses);
        verify(this.expenseRepository).findReceivedBook(fromDate, toDate, INVESTMENT_ASSET_THRESHOLD_DECIMAL);
    }

    @Test
    void shouldCountInvoiceReceivedBook() {
        LocalDate fromDate = LocalDate.of(2026, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 3, 31);
        when(this.expenseRepository.countReceivedBook(fromDate, toDate, INVESTMENT_ASSET_THRESHOLD_DECIMAL)).thenReturn(2L);

        assertEquals(2L, this.expensePersistenceMongodb
                .countInvoiceReceivedBook(fromDate, toDate, INVESTMENT_ASSET_THRESHOLD));
        verify(this.expenseRepository).countReceivedBook(fromDate, toDate, INVESTMENT_ASSET_THRESHOLD_DECIMAL);
    }

    @Test
    void shouldFindNextNumberForCurrentDepreciationRate() {
        ExpenseEntity lastCurrent = ExpenseEntity.builder()
                .series("2026")
                .number(7)
                .depreciationRate(100)
                .build();
        when(this.expenseRepository.findFirstBySeriesAndDepreciationRateOrderByNumberDesc("2026", 100))
                .thenReturn(Optional.of(lastCurrent));

        Integer next = this.expensePersistenceMongodb.findNextNumber("2026", 100);

        assertEquals(8, next);
        verify(this.expenseRepository).findFirstBySeriesAndDepreciationRateOrderByNumberDesc("2026", 100);
    }

    @Test
    void shouldFindNextNumberForCapitalDepreciationRatesAsSingleBucket() {
        ExpenseEntity lastCapital = ExpenseEntity.builder()
                .series("2026")
                .number(12)
                .depreciationRate(10)
                .build();
        when(this.expenseRepository.findFirstBySeriesAndDepreciationRateNotOrderByNumberDesc("2026", 100))
                .thenReturn(Optional.of(lastCapital));

        Integer next = this.expensePersistenceMongodb.findNextNumber("2026", 20);

        assertEquals(13, next);
        verify(this.expenseRepository).findFirstBySeriesAndDepreciationRateNotOrderByNumberDesc("2026", 100);
    }
}
