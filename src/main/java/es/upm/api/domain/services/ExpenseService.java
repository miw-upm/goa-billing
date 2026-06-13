package es.upm.api.domain.services;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.SupplierInfo;
import es.upm.api.domain.model.criteria.ExpenseFindCriteria;
import es.upm.api.domain.ports.out.billing.ExpenseGateway;
import es.upm.api.domain.ports.out.engagement.EngagementGateway;
import es.upm.miw.exception.ClientBusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ExpenseService {
    private final ExpenseGateway expenseGateway;
    private final EngagementGateway engagementGateway;

    public void create(Expense expense) {
        expense.setId(UUID.randomUUID());
        expense.setRecordedAt(LocalDateTime.now());
        if (expense.getEngagement() != null) {
            expense.setEngagement(this.engagementGateway.read(expense.getEngagement().getId()));
        }
        String series = String.valueOf(LocalDate.now().getYear());
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
        expense.setRecordedAt(LocalDateTime.now());
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
        LocalDate endOfQuarter = expenseDate
                .with(expenseDate.getMonth().firstMonthOfQuarter())
                .plusMonths(3)
                .minusDays(1);
        if (LocalDate.now().isAfter(endOfQuarter)) {
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
