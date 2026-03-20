package es.upm.api.infrastructure.mongodb.persistence;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.persistence.ExpensePersistence;
import es.upm.api.infrastructure.mongodb.entities.ExpenseEntity;
import es.upm.api.infrastructure.mongodb.repositories.ExpenseRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ExpensePersistenceMongodb implements ExpensePersistence {

    private final ExpenseRepository expenseRepository;

    public ExpensePersistenceMongodb(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @Override
    public void create(Expense expense) {
        this.expenseRepository.save(new ExpenseEntity(expense));
    }
}