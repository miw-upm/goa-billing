package es.upm.api.adapter.out.billing.mongo.expense;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends MongoRepository<ExpenseEntity, String> {
    List<ExpenseEntity> findAllByOrderBySeriesDescNumberDesc();

    List<ExpenseEntity> findBySupplierNameContainingIgnoreCaseOrSupplierIdentityContainingIgnoreCase(
            String supplierName, String supplierIdentity
    );

    List<ExpenseEntity> findBySupplierNameContainingIgnoreCaseOrSupplierIdentityContainingIgnoreCaseOrderBySeriesDescNumberDesc(
            String supplierName, String supplierIdentity
    );

    List<ExpenseEntity> findByEngagementIdStartingWithOrderBySeriesDescNumberDesc(String engagementIdPrefix);

    List<ExpenseEntity> findByEngagementIdOrderByIssueDateDesc(String engagementId);

    Optional<ExpenseEntity> findFirstBySeriesAndDepreciationRateOrderByNumberDesc(String series, Integer depreciationRate);

    Optional<ExpenseEntity> findFirstBySeriesAndDepreciationRateNotOrderByNumberDesc(String series, Integer depreciationRate);
}
