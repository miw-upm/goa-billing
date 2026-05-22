package es.upm.api.domain.services;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.criteria.ExpenseFindCriteria;
import es.upm.api.domain.ports.out.billing.ExpenseGateway;
import es.upm.api.domain.ports.out.engagement.EngagementFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ExpenseService {
    private final ExpenseGateway expenseGateway;
    private final EngagementFinder engagementFinder;

    public Expense create(Expense expense) {
        expense.setId(UUID.randomUUID());
        expense.setDate(LocalDate.now());
        expense.setEngagement(this.engagementFinder.read(expense.getEngagement().getId()));
        expense.setDocumentPath(null); //TODO
        this.expenseGateway.create(expense);
        return expense;
    }

    public Expense read(UUID id) {
        Expense expense = this.expenseGateway.read(id);
        expense.setEngagement(this.engagementFinder.read(expense.getEngagement().getId()));
        return expense;
    }

    public Expense update(UUID id, Expense expense) {
        Expense currentExpense = this.expenseGateway.read(id);
        expense.setId(id);
        expense.setDate(currentExpense.getDate());
        expense.setDocumentPath(currentExpense.getDocumentPath());
        expense.setEngagement(this.engagementFinder.read(expense.getEngagement().getId()));
        return this.expenseGateway.update(id, expense);
    }

    public void delete(UUID id) {
        this.expenseGateway.delete(id);
    }

    public Stream<Expense> find(ExpenseFindCriteria criteria) {
        return this.expenseGateway.find(criteria);
    }
}
