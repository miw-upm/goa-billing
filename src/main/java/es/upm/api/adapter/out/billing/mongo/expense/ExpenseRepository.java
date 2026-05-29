package es.upm.api.adapter.out.billing.mongo.expense;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends MongoRepository<ExpenseEntity, String> {
    List<ExpenseEntity> findAllByOrderByIssueDateDesc();

    List<ExpenseEntity> findBySupplierNameContainingIgnoreCaseOrSupplierIdentityContainingIgnoreCase(
            String supplierName, String supplierIdentity
    );

    List<ExpenseEntity> findBySupplierNameContainingIgnoreCaseOrSupplierIdentityContainingIgnoreCaseOrderByIssueDateDesc(
            String supplierName, String supplierIdentity
    );

    List<ExpenseEntity> findByIssueDateGreaterThanEqualOrderByIssueDateDesc(LocalDate issueDate);

    List<ExpenseEntity> findByEngagementIdCode64StartingWithOrderByIssueDateDesc(String engagementIdCode64Prefix);

    List<ExpenseEntity> findByEngagementIdOrderByIssueDateDesc(String engagementId);
}
