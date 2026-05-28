package es.upm.api.adapter.out.billing.mongo.expense;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ExpenseRepository extends MongoRepository<ExpenseEntity, UUID> {
    List<ExpenseEntity> findAllByOrderByIssueDateDesc();

    List<ExpenseEntity> findBySupplierNameContainingIgnoreCaseOrSupplierIdentityContainingIgnoreCase(
            String supplierName, String supplierIdentity
    );

    List<ExpenseEntity> findBySupplierNameContainingIgnoreCaseOrSupplierIdentityContainingIgnoreCaseOrderByIssueDateDesc(
            String supplierName, String supplierIdentity
    );

    List<ExpenseEntity> findByIssueDateGreaterThanEqualOrderByIssueDateDesc(LocalDate issueDate);

    List<ExpenseEntity> findByEngagementIdCode64StartingWithOrderByIssueDateDesc(String engagementIdCode64Prefix);

    List<ExpenseEntity> findByEngagementIdOrderByIssueDateDesc(UUID engagementId);
}
