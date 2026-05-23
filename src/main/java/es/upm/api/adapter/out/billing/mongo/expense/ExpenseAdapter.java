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

        expenseEntity.setEngagementId(expense.getEngagement().getId());
        expenseEntity.setBaseAmount(expense.getBaseAmount());
        expenseEntity.setVatRate(expense.getVatRate());
        expenseEntity.setSupplier(expense.getSupplier());
        expenseEntity.setSupplierIdentity(expense.getSupplierIdentity());
        expenseEntity.setTaxCategory(expense.getTaxCategory());
        expenseEntity.setDate(expense.getIssueDate());
        expenseEntity.setDocumentPath(expense.getDocumentPath());

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
            result = this.expenseRepository.findAll(DATE);
        } else if (criteria.getEngagementId() != null) {
            result = this.expenseRepository.findByEngagementId(criteria.getEngagementId());
            if (criteria.getFromDate() != null) {
                result = result.stream()
                        .filter(expenseEntity -> expenseEntity.getDate().equals(criteria.getFromDate()))
                        .toList();
            }
        } else {
            result = this.expenseRepository.findByDate(criteria.getFromDate());
        }

        return result.stream()
                .map(ExpenseEntity::toDomain);
    }
}
