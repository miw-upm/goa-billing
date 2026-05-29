package es.upm.api.domain.services;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.ExpenseType;
import es.upm.api.domain.model.SupplierInfo;
import es.upm.api.domain.model.TaxCategory;
import es.upm.api.domain.model.criteria.ExpenseFindCriteria;
import es.upm.api.domain.model.external.EngagementSnapshot;
import es.upm.api.domain.ports.out.billing.ExpenseGateway;
import es.upm.api.domain.ports.out.engagement.EngagementGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class ExpenseServiceIT {

    private final ExpenseFindCriteria criteria = new ExpenseFindCriteria();
    @Autowired
    private ExpenseService expenseService;
    @MockitoBean
    private ExpenseGateway expenseGateway;
    @MockitoBean
    private EngagementGateway engagementGateway;
    private Expense expense;
    private UUID engagementId;
    private EngagementSnapshot engagement;

    @BeforeEach
    void setUp() {
        this.engagementId = UUID.randomUUID();
        this.engagement = EngagementSnapshot.builder().id(this.engagementId).build();
        this.expense = Expense.builder()
                .engagement(this.engagement)
                .baseAmount(BigDecimal.valueOf(25))
                .vatRate(21)
                .supplier(SupplierInfo.builder().name("Taxi Madrid").identity("A10000000").build())
                .taxCategory(TaxCategory.OTROS)
                .expenseType(ExpenseType.CURRENT)
                .issueDate(LocalDate.of(2026, 3, 20))
                .withholdingTax(BigDecimal.ZERO)
                .documentPath("doc/path")
                .build();
    }


    @Test
    void shouldNotPersistExpenseWhenEngagementDoesNotExist() {
        RuntimeException exception = new RuntimeException("Engagement not found");
        when(this.engagementGateway.read(this.engagementId)).thenThrow(exception);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> this.expenseService.create(this.expense));

        assertEquals("Engagement not found", thrown.getMessage());
        verify(this.engagementGateway).read(this.engagementId);
        verify(this.expenseGateway, never()).create(any());
    }

    @Test
    void shouldReadExpenseById() {
        this.expense.setId(UUID.randomUUID());
        when(this.expenseGateway.read(this.expense.getId())).thenReturn(this.expense);
        when(this.engagementGateway.read(this.engagementId)).thenReturn(this.engagement);

        Expense readExpense = this.expenseService.read(this.expense.getId());

        assertEquals(this.expense, readExpense);
        verify(this.expenseGateway).read(this.expense.getId());
        verify(this.engagementGateway).read(this.engagementId);
    }

    @Test
    void shouldFind() {
        Stream<Expense> expenseStream = Stream.of(this.expense);
        when(this.expenseGateway.find(this.criteria)).thenReturn(expenseStream);

        Stream<Expense> allExpenses = this.expenseService.find(this.criteria);

        verify(this.expenseGateway).find(this.criteria);
        assertEquals(this.expense, allExpenses.findFirst().orElse(null));
    }


    @Test
    void shouldNotUpdateExpenseWhenEngagementDoesNotExist() {
        UUID id = UUID.randomUUID();
        Expense existing = Expense.builder()
                .id(id)
                .engagement(this.engagement)
                .baseAmount(BigDecimal.TEN)
                .vatRate(21)
                .supplier(SupplierInfo.builder().name("Old").identity("OLD").build())
                .taxCategory(TaxCategory.OTROS)
                .expenseType(ExpenseType.CURRENT)
                .issueDate(LocalDate.of(2026, 3, 20))
                .documentPath("old/doc")
                .build();
        Expense updateData = Expense.builder()
                .engagement(this.engagement)
                .baseAmount(BigDecimal.valueOf(90))
                .vatRate(21)
                .supplier(SupplierInfo.builder().name("Updated supplier").identity("NEW").build())
                .taxCategory(TaxCategory.SUMINISTROS)
                .expenseType(ExpenseType.CAPITAL)
                .build();
        RuntimeException exception = new RuntimeException("Engagement not found");
        when(this.expenseGateway.read(id)).thenReturn(existing);
        when(this.engagementGateway.read(this.engagementId)).thenThrow(exception);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> this.expenseService.update(id, updateData));

        assertEquals("Engagement not found", thrown.getMessage());
        verify(this.expenseGateway).read(id);
        verify(this.engagementGateway).read(this.engagementId);
        verify(this.expenseGateway, never()).update(any(), any());
    }

}
