package es.upm.api.adapter.out.billing.mongo.invoice;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends MongoRepository<InvoiceEntity, String> {
    List<InvoiceEntity> findAllByOrderByEmissionDateDesc();

    List<InvoiceEntity> findByEmissionDateGreaterThanEqualOrderByEmissionDateDesc(LocalDate emissionDate);

    List<InvoiceEntity> findByEngagementIdStartingWithOrderByEmissionDateDesc(String engagementIdPrefix);

    List<InvoiceEntity> findByEngagementIdStartingWithAndEmissionDateGreaterThanEqualOrderByEmissionDateDesc(
            String engagementIdPrefix, LocalDate emissionDate);

    @Query(value = """
            {
              'emissionDate': { $ne: null },
              '$or': [
                { 'operationDate': { $gte: ?0, $lte: ?1 } },
                { 'operationDate': null, 'emissionDate': { $gte: ?0, $lte: ?1 } }
              ]
            }
            """, sort = "{ 'number': 1 }")
    List<InvoiceEntity> findIssuedBetweenOrderByNumberAsc(LocalDate fromDate, LocalDate toDate);

    @Query(value = """
            {
              'series': ?0,
              'number': { $gte: ?1, $lte: ?2 },
              'emissionDate': { $ne: null }
            }
            """, sort = "{ 'number': 1 }")
    List<InvoiceEntity> findIssuedBetweenOrderByNumberAsc(String series, int fromNumber, int toNumber);

    Optional<InvoiceEntity> findFirstBySeriesOrderByNumberDesc(String series);

    Optional<InvoiceEntity> findBySeriesAndNumber(String series, Integer number);
}
