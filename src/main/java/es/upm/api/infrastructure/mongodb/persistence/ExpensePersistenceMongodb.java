package es.upm.api.infrastructure.mongodb.persistence;

import es.upm.api.domain.exceptions.NotFoundException;
import es.upm.api.domain.model.Expense;
import es.upm.api.domain.persistence.ExpensePersistence;
import es.upm.api.infrastructure.mongodb.entities.ExpenseEntity;
import es.upm.api.infrastructure.mongodb.repositories.ExpenseRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

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

    @Override
    public Expense readById(UUID id) {
        return this.expenseRepository.findById(id)
                .map(ExpenseEntity::toExpense)
                .orElseThrow(() -> new NotFoundException("Expense id: " + id));
    }
}
