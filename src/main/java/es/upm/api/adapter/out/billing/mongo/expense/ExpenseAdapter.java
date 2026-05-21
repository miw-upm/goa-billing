package es.upm.api.adapter.out.billing.mongo.expense;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.criteria.ExpenseFindCriteria;
import es.upm.api.domain.ports.out.billing.ExpenseGateway;
import es.upm.miw.exception.NotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
public class ExpenseAdapter implements ExpenseGateway {
    public static final Sort DATE = Sort.by(Sort.Direction.DESC, "date");

    private final ExpenseRepository expenseRepository;

    public ExpenseAdapter(ExpenseRepository expenseRepository) {
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

        return this.expenseRepository.save(expenseEntity).toDomain();
    }

    @Override
    public Expense readById(UUID id) {
        return this.expenseRepository.findById(id)
                .map(ExpenseEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("Expense id: " + id));
    }

    public Stream<Expense> findAll(ExpenseFindCriteria criteria) {
        List<ExpenseEntity> result;

        if (criteria.isEmpty()) {
            result = this.expenseRepository.findAll(DATE);
        } else if (criteria.getEngagementId() != null) {
            result = this.expenseRepository.findByEngagementId(criteria.getEngagementId());
            if (criteria.getDate() != null) {
                result = result.stream()
                        .filter(expenseEntity -> expenseEntity.getDate().equals(criteria.getDate()))
                        .toList();
            }
        } else {
            result = this.expenseRepository.findByDate(criteria.getDate());
        }

        return result.stream()
                .map(ExpenseEntity::toDomain);
    }
}
