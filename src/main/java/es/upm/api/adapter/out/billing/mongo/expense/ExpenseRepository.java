package es.upm.api.adapter.out.billing.mongo.expense;

import es.upm.api.domain.model.ExpenseType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends MongoRepository<ExpenseEntity, String> {
    List<ExpenseEntity> findAllByOrderByIssueDateDesc();

    List<ExpenseEntity> findBySupplierNameContainingIgnoreCaseOrSupplierIdentityContainingIgnoreCase(
            String supplierName, String supplierIdentity
    );

    List<ExpenseEntity> findBySupplierNameContainingIgnoreCaseOrSupplierIdentityContainingIgnoreCaseOrderByIssueDateDesc(
            String supplierName, String supplierIdentity
    );

    List<ExpenseEntity> findByIssueDateGreaterThanEqualOrderByIssueDateDesc(LocalDate issueDate);

    List<ExpenseEntity> findByEngagementIdStartingWithOrderByIssueDateDesc(String engagementIdPrefix);

    List<ExpenseEntity> findByEngagementIdOrderByIssueDateDesc(String engagementId);

    Optional<ExpenseEntity> findFirstBySeriesAndExpenseTypeOrderByNumberDesc(String series, ExpenseType expenseType);
}
