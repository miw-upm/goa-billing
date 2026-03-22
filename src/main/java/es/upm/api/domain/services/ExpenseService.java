package es.upm.api.domain.services;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.persistence.ExpensePersistence;
import es.upm.api.domain.webclients.EngagementWebClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ExpenseService {

    private final ExpensePersistence expensePersistence;
    private final EngagementWebClient engagementWebClient;

    public ExpenseService(ExpensePersistence expensePersistence, EngagementWebClient engagementWebClient) {
        this.expensePersistence = expensePersistence;
        this.engagementWebClient = engagementWebClient;
    }

    public Expense create(Expense expense) {
        expense.setId(UUID.randomUUID());
        this.engagementWebClient.readById(expense.getEngagementId());
        this.expensePersistence.create(expense);
        return expense;
    }

    public Expense readById(UUID id) {
        return this.expensePersistence.readById(id);
    }
}
