package es.upm.api.adapter.out.billing.mongo.invoice;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends MongoRepository<InvoiceEntity, String> {
    List<InvoiceEntity> findAllByOrderBySeriesDescNumberDesc();

    List<InvoiceEntity> findByEmissionDateGreaterThanEqualOrderBySeriesDescNumberDesc(LocalDate emissionDate);

    List<InvoiceEntity> findByEngagementIdStartingWithOrderBySeriesDescNumberDesc(String engagementIdPrefix);

    List<InvoiceEntity> findByEngagementIdStartingWithAndEmissionDateGreaterThanEqualOrderBySeriesDescNumberDesc(
            String engagementIdPrefix, LocalDate emissionDate);

    @Query(value = "{ 'emissionDate': { $gte: ?0, $lte: ?1 } }", sort = "{ 'number': 1 }")
    List<InvoiceEntity> findByEmissionDateRange(LocalDate fromDate, LocalDate toDate);

    @Query(value = """
            {
              'series': ?0,
              'number': { $gte: ?1, $lte: ?2 },
              'emissionDate': { $ne: null }
            }
            """, sort = "{ 'number': 1 }")
    List<InvoiceEntity> findIssuedBySeriesAndNumberRange(String series, int fromNumber, int toNumber);

    Optional<InvoiceEntity> findFirstBySeriesOrderByNumberDesc(String series);

    Optional<InvoiceEntity> findBySeriesAndNumber(String series, Integer number);
}
