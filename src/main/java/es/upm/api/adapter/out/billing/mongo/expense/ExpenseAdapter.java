package es.upm.api.adapter.out.billing.mongo.expense;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.criteria.ExpenseFindCriteria;
import es.upm.api.domain.ports.out.billing.ExpenseGateway;
import es.upm.miw.exception.BadRequestException;
import es.upm.miw.exception.NotFoundException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
public class ExpenseAdapter implements ExpenseGateway {
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
        this.expenseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Expense id: " + id));
        if (!Objects.equals(id, expense.getId())) {
            throw new BadRequestException("Expense id mismatch: path id " + id + " and body id " + expense.getId());
        }
        ExpenseEntity expenseEntity = new ExpenseEntity(expense);
        return this.expenseRepository.save(expenseEntity).toDomain();
    }

    @Override
    public Expense read(UUID id) {
        return this.expenseRepository.findById(id)
                .map(ExpenseEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("Expense id: " + id));
    }

    @Override
    public void delete(UUID id) {
        ExpenseEntity expenseEntity = this.expenseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Expense id: " + id));
        this.expenseRepository.delete(expenseEntity);
    }

    @Override
    public Stream<Expense> find(ExpenseFindCriteria criteria) {
        List<ExpenseEntity> result;
        if (criteria.isEmpty()) {
            result = this.expenseRepository.findAllByOrderByIssueDateDesc();
        } else if (criteria.getEngagementId() != null && criteria.getFromDate() != null) {
            result = this.expenseRepository.findByEngagementIdAndIssueDateGreaterThanEqualOrderByIssueDateDesc(
                    criteria.getEngagementId(), criteria.getFromDate()
            );
        } else if (criteria.getEngagementId() != null) {
            result = this.expenseRepository.findByEngagementIdOrderByIssueDateDesc(criteria.getEngagementId());
        } else {
            result = this.expenseRepository.findByIssueDateGreaterThanEqualOrderByIssueDateDesc(criteria.getFromDate());
        }
        return result.stream()
                .map(ExpenseEntity::toDomain);
    }
}
