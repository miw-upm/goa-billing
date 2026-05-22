package es.upm.api.domain.services;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.criteria.ExpenseFindCriteria;
import es.upm.api.domain.ports.out.billing.ExpenseGateway;
import es.upm.api.adapter.out.engagement.feign.EngagementWebClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ExpenseService {
    private final ExpenseGateway expenseGateway;
    private final EngagementWebClient engagementWebClient;

    public Expense create(Expense expense) {
        expense.setId(UUID.randomUUID());
        expense.setDate(LocalDate.now());
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
