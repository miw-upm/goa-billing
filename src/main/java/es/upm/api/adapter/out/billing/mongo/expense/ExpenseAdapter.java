package es.upm.api.adapter.out.billing.mongo.expense;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.SupplierInfo;
import es.upm.api.domain.model.criteria.ExpenseFindCriteria;
import es.upm.api.domain.ports.out.billing.ExpenseGateway;
import es.upm.miw.exception.BadRequestException;
import es.upm.miw.exception.NotFoundException;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
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
        if (StringUtils.hasText(criteria.getEngagementReference())) {
            result = this.expenseRepository.findByEngagementIdCode64StartingWithOrderByIssueDateDesc(
                    this.normalizeEngagementReference(criteria.getEngagementReference())
            );
        } else if (criteria.getSupplier() == null) {
            result = this.expenseRepository.findAllByOrderByIssueDateDesc();
        } else {
            result = this.expenseRepository.findBySupplierNameContainingIgnoreCaseOrSupplierIdentityContainingIgnoreCaseOrderByIssueDateDesc(
                    criteria.getSupplier(), criteria.getSupplier()
            );
        }
        Stream<ExpenseEntity> stream = result.stream();
        if (StringUtils.hasText(criteria.getEngagementReference()) && StringUtils.hasText(criteria.getSupplier())) {
            String supplier = criteria.getSupplier().toLowerCase(Locale.ROOT);
            stream = stream.filter(expenseEntity ->
                    expenseEntity.getSupplier() != null
                            && ((expenseEntity.getSupplier().getName() != null
                            && expenseEntity.getSupplier().getName().toLowerCase(Locale.ROOT).contains(supplier))
                            || (expenseEntity.getSupplier().getIdentity() != null
                            && expenseEntity.getSupplier().getIdentity().toLowerCase(Locale.ROOT).contains(supplier))));
        }
        if (criteria.getCategory() != null) {
            String category = criteria.getCategory().toLowerCase(Locale.ROOT);
            stream = stream.filter(expenseEntity -> expenseEntity.getTaxCategory() != null
                    && expenseEntity.getTaxCategory().name().toLowerCase(Locale.ROOT).contains(category));
        }
        if (criteria.getFromDate() != null) {
            LocalDate fromDate = criteria.getFromDate();
            stream = stream.filter(expenseEntity -> expenseEntity.getIssueDate() != null
                    && !expenseEntity.getIssueDate().isBefore(fromDate));
        }
        return stream.map(ExpenseEntity::toDomain);
    }

    private String normalizeEngagementReference(String engagementReference) {
        return engagementReference.trim().replace("=", "");
    }

    @Override
    public Stream<Expense> findByEngagementId(UUID engagementId) {
        return this.expenseRepository.findByEngagementIdOrderByIssueDateDesc(engagementId).stream()
                .map(ExpenseEntity::toDomain);
    }

    @Override
    public Stream<SupplierInfo> findSuppliers(String supplier) {
        String normalizedSupplier = Objects.toString(supplier, "");
        return this.expenseRepository.findBySupplierNameContainingIgnoreCaseOrSupplierIdentityContainingIgnoreCase(
                        normalizedSupplier, normalizedSupplier
                ).stream()
                .map(ExpenseEntity::getSupplier)
                .filter(Objects::nonNull)
                .distinct();
    }
}
