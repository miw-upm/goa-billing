package es.upm.api.domain.services;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.SupplierInfo;
import es.upm.api.domain.model.criteria.ExpenseFindCriteria;
import es.upm.api.domain.ports.out.billing.ExpenseGateway;
import es.upm.api.domain.ports.out.engagement.EngagementGateway;
import es.upm.miw.exception.ClientBusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class ExpenseService {
    private static final int QUARTER_CLOSE_DAY = 20;
    private static final int FOURTH_QUARTER_CLOSE_DAY = 30;
    private final ExpenseGateway expenseGateway;
    private final EngagementGateway engagementGateway;
    private final Clock clock;

    @Autowired
    public ExpenseService(ExpenseGateway expenseGateway, EngagementGateway engagementGateway) {
        this(expenseGateway, engagementGateway, Clock.systemDefaultZone());
    }

    ExpenseService(ExpenseGateway expenseGateway, EngagementGateway engagementGateway, Clock clock) {
        this.expenseGateway = expenseGateway;
        this.engagementGateway = engagementGateway;
        this.clock = clock;
    }

    public void create(Expense expense) {
        expense.setId(UUID.randomUUID());
        expense.setRecordedAt(LocalDateTime.now(this.clock));
        if (expense.getEngagement() != null) {
            expense.setEngagement(this.engagementGateway.read(expense.getEngagement().getId()));
        }
        String series = String.valueOf(LocalDate.now(this.clock).getYear());
        if (Objects.isNull(expense.getNumber())) {
            expense.setSeries(series);
            expense.setNumber(this.expenseGateway.findNextNumber(series, expense.getDepreciationRate()));
        }
        expense.setDocumentPath(null); //TODO
        this.expenseGateway.create(expense);
    }

    public Expense read(UUID id) {
        Expense expense = this.expenseGateway.read(id);
        if (expense.getEngagement() != null) {
            expense.setEngagement(this.engagementGateway.read(expense.getEngagement().getId()));
        }
        return expense;
    }

    public void update(UUID id, Expense expense) {
        Expense currentExpense = this.expenseGateway.read(id);
        this.assertQuarterOpen(currentExpense);
        expense.setId(id);
        expense.setRecordedAt(LocalDateTime.now(this.clock));
        expense.setSeries(currentExpense.getSeries());
        expense.setNumber(currentExpense.getNumber());
        expense.setDocumentPath(currentExpense.getDocumentPath());
        if (expense.getEngagement() != null) {
            expense.setEngagement(this.engagementGateway.read(expense.getEngagement().getId()));
        }
        this.expenseGateway.update(id, expense);
    }

    private void assertQuarterOpen(Expense expense) {
        LocalDate expenseDate = expense.getIssueDate();
        Month firstMonthOfQuarter = expenseDate.getMonth().firstMonthOfQuarter();
        LocalDate firstDayAfterQuarter = LocalDate.of(expenseDate.getYear(), firstMonthOfQuarter, 1)
                .plusMonths(3);
        int closeDay = firstMonthOfQuarter == Month.OCTOBER ? FOURTH_QUARTER_CLOSE_DAY : QUARTER_CLOSE_DAY;
        LocalDate quarterCloseDate = firstDayAfterQuarter.withDayOfMonth(closeDay);

        if (LocalDate.now(this.clock).isAfter(quarterCloseDate)) {
            throw new ClientBusinessException(
                    "No se puede modificar un gasto de un trimestre ya cerrado: "
                            + expense.getSeries() + "-" + expense.getNumber());
        }
    }

    public Stream<Expense> find(ExpenseFindCriteria criteria) {
        return this.expenseGateway.find(criteria)
                .map(expense -> {
                    if (expense.getEngagement() != null) {
                        expense.setEngagement(this.engagementGateway.read(expense.getEngagement().getId()));
                    }
                    return expense;
                });
    }

    public Stream<SupplierInfo> findSuppliers(String supplier) {
        return this.expenseGateway.findSuppliers(supplier);
    }
}
