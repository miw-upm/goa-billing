package es.upm.api.infrastructure.mongodb.persistence;

import es.upm.api.domain.exceptions.NotFoundException;
import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.ExpenseFindCriteria;
import es.upm.api.domain.persistence.ExpensePersistence;
import es.upm.api.infrastructure.mongodb.entities.ExpenseEntity;
import es.upm.api.infrastructure.mongodb.repositories.ExpenseRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
public class ExpensePersistenceMongodb implements ExpensePersistence {
    public static final Sort DATE = Sort.by(Sort.Direction.DESC, "date");

    private final ExpenseRepository expenseRepository;

    public ExpensePersistenceMongodb(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @Override
    public void create(Expense expense) {
        this.expenseRepository.save(new ExpenseEntity(expense));
    }

    @Override
    public Expense update(UUID id, Expense expense) {
        ExpenseEntity expenseEntity = this.expenseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Expense id: " + id));

        expenseEntity.setEngagementId(expense.getEngagementId());
        expenseEntity.setAmount(expense.getAmount());
        expenseEntity.setDate(expense.getDate());
        expenseEntity.setDescription(expense.getDescription());

        return this.expenseRepository.save(expenseEntity).toExpense();
    }

    @Override
    public Expense readById(UUID id) {
        return this.expenseRepository.findById(id)
                .map(ExpenseEntity::toExpense)
                .orElseThrow(() -> new NotFoundException("Expense id: " + id));
    }

    public Stream<Expense> findAll(ExpenseFindCriteria criteria) {
        List<ExpenseEntity> result;

        if (criteria.getDate() != null) {
            result = this.expenseRepository.findByDate(criteria.getDate());
        } else {
            result = this.expenseRepository.findAll(DATE);
        }

        return result.stream()
                .map(ExpenseEntity::toExpense);
    }
}
