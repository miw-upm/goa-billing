package es.upm.api.domain.services;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.criteria.ExpenseFindCriteria;
import es.upm.api.domain.ports.out.billing.ExpenseGateway;
import es.upm.api.domain.ports.out.engagement.EngagementWebClient;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.stream.Stream;

@Service
public class ExpenseService {

    private final ExpenseGateway expenseGateway;
    private final EngagementWebClient engagementWebClient;

    public ExpenseService(ExpenseGateway expenseGateway, EngagementWebClient engagementWebClient) {
        this.expenseGateway = expenseGateway;
        this.engagementWebClient = engagementWebClient;
    }

    public Expense create(Expense expense) {
        expense.setId(UUID.randomUUID());
        this.engagementWebClient.readById(expense.getEngagementId());
        this.expenseGateway.create(expense);
        return expense;
    }

    public Expense update(UUID id, Expense expense) {
        this.engagementWebClient.readById(expense.getEngagementId());
        return this.expenseGateway.update(id, expense);
    }

    public Expense read(UUID id) {
        return this.expenseGateway.readById(id);
    }

    public Stream<Expense> find(ExpenseFindCriteria criteria) {
        return this.expenseGateway.findAll(criteria);
    }
}
