package es.upm.api.domain.services;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.SupplierInfo;
import es.upm.api.domain.model.TaxCategory;
import es.upm.api.domain.model.criteria.ExpenseFindCriteria;
import es.upm.api.domain.model.external.EngagementSnapshot;
import es.upm.api.domain.ports.out.billing.ExpenseGateway;
import es.upm.api.domain.ports.out.engagement.EngagementGateway;
import es.upm.miw.exception.ClientBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
                .depreciationRate(100)
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
    void shouldUpdateExpenseUntilDay20OfMonthAfterQuarter() {
        ExpenseService service = this.expenseServiceAt(LocalDate.of(2026, 4, 20));
        UUID expenseId = UUID.randomUUID();
        this.expense.setSeries("2026");
        this.expense.setNumber(1);
        this.expense.setIssueDate(LocalDate.of(2026, 3, 20));
        when(this.expenseGateway.read(expenseId)).thenReturn(this.expense);

        service.update(expenseId, Expense.builder()
                .baseAmount(BigDecimal.valueOf(35))
                .vatRate(21)
                .supplier(this.expense.getSupplier())
                .taxCategory(TaxCategory.OTROS)
                .depreciationRate(100)
                .issueDate(LocalDate.of(2026, 3, 21))
                .build());

        verify(this.expenseGateway).update(eq(expenseId), any(Expense.class));
    }

    @Test
    void shouldNotUpdateExpenseAfterDay20OfMonthAfterQuarter() {
        ExpenseService service = this.expenseServiceAt(LocalDate.of(2026, 4, 21));
        UUID expenseId = UUID.randomUUID();
        this.expense.setSeries("2026");
        this.expense.setNumber(1);
        this.expense.setIssueDate(LocalDate.of(2026, 3, 20));
        when(this.expenseGateway.read(expenseId)).thenReturn(this.expense);

        assertThrows(ClientBusinessException.class, () -> service.update(expenseId, this.expense));

        verify(this.expenseGateway, never()).update(any(), any());
    }

    @Test
    void shouldUpdateFourthQuarterExpenseUntilJanuary30() {
        ExpenseService service = this.expenseServiceAt(LocalDate.of(2027, 1, 30));
        UUID expenseId = UUID.randomUUID();
        this.expense.setSeries("2026");
        this.expense.setNumber(4);
        this.expense.setIssueDate(LocalDate.of(2026, 12, 31));
        when(this.expenseGateway.read(expenseId)).thenReturn(this.expense);

        service.update(expenseId, Expense.builder()
                .baseAmount(BigDecimal.valueOf(35))
                .vatRate(21)
                .supplier(this.expense.getSupplier())
                .taxCategory(TaxCategory.OTROS)
                .depreciationRate(100)
                .issueDate(LocalDate.of(2026, 12, 30))
                .build());

        verify(this.expenseGateway).update(eq(expenseId), any(Expense.class));
    }

    @Test
    void shouldNotUpdateFourthQuarterExpenseAfterJanuary30() {
        ExpenseService service = this.expenseServiceAt(LocalDate.of(2027, 1, 31));
        UUID expenseId = UUID.randomUUID();
        this.expense.setSeries("2026");
        this.expense.setNumber(4);
        this.expense.setIssueDate(LocalDate.of(2026, 12, 31));
        when(this.expenseGateway.read(expenseId)).thenReturn(this.expense);

        assertThrows(ClientBusinessException.class, () -> service.update(expenseId, this.expense));

        verify(this.expenseGateway, never()).update(any(), any());
    }

    private ExpenseService expenseServiceAt(LocalDate date) {
        ZoneId zone = ZoneId.systemDefault();
        return new ExpenseService(
                this.expenseGateway,
                this.engagementGateway,
                Clock.fixed(date.atStartOfDay(zone).toInstant(), zone)
        );
    }

}
