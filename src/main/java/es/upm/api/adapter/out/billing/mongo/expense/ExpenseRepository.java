package es.upm.api.adapter.out.billing.mongo.expense;

import org.bson.types.Decimal128;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

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

    @Query(value = """
            {
              'issueDate': { $gte: ?0, $lte: ?1 },
              'vatRate': { $gt: 0 },
              '$or': [
                { 'depreciationRate': 100 },
                { 'depreciationRate': { $ne: 100 }, 'baseAmount': { $lt: ?2 } }
              ]
            }
            """, sort = "{ 'issueDate': 1 }")
    List<ExpenseEntity> findReceivedBook(LocalDate fromDate, LocalDate toDate, Decimal128 taxableBaseThreshold);

    @Query(value = """
            {
              'issueDate': { $gte: ?0, $lte: ?1 },
              'vatRate': { $gt: 0 },
              'depreciationRate': { $ne: 100 },
              'baseAmount': { $gt: ?2 }
            }
            """, sort = "{ 'issueDate': 1 }")
    List<ExpenseEntity> findReceivedInvestmentBook(LocalDate fromDate, LocalDate toDate, Decimal128 taxableBaseThreshold);

    @Query(value = """
            {
              'issueDate': { $lte: ?0 },
              'depreciationRate': { $ne: 100 },
              'baseAmount': { $gt: ?1 }
            }
            """, sort = "{ 'issueDate': 1 }")
    List<ExpenseEntity> findInvestmentAssetsUntil(LocalDate toDate, Decimal128 taxableBaseThreshold);

    @Query(value = """
            {
              'issueDate': { $gte: ?0, $lte: ?1 },
              'vatRate': { $gt: 0 },
              '$or': [
                { 'depreciationRate': 100 },
                { 'depreciationRate': { $ne: 100 }, 'baseAmount': { $lt: ?2 } }
              ]
            }
            """, count = true)
    long countReceivedBook(LocalDate fromDate, LocalDate toDate, Decimal128 taxableBaseThreshold);
}
