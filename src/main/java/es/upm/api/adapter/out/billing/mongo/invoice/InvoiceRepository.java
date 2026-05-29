package es.upm.api.adapter.out.billing.mongo.invoice;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends MongoRepository<InvoiceEntity, String> {
    List<InvoiceEntity> findAllByOrderByEmissionDateDesc();

    List<InvoiceEntity> findByEmissionDateGreaterThanEqualOrderByEmissionDateDesc(LocalDate emissionDate);

    List<InvoiceEntity> findByEngagementIdStartingWithOrderByEmissionDateDesc(String engagementIdPrefix);

    List<InvoiceEntity> findByEngagementIdStartingWithAndEmissionDateGreaterThanEqualOrderByEmissionDateDesc(
            String engagementIdPrefix, LocalDate emissionDate);

    Optional<InvoiceEntity> findFirstBySeriesOrderByNumberDesc(String series);
}
