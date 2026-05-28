package es.upm.api.adapter.out.billing.mongo.invoice;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends MongoRepository<InvoiceEntity, UUID> {
    List<InvoiceEntity> findAllByOrderByEmissionDateDesc();

    List<InvoiceEntity> findByEmissionDateGreaterThanEqualOrderByEmissionDateDesc(LocalDate emissionDate);

    List<InvoiceEntity> findByEngagementIdCode64StartingWithOrderByEmissionDateDesc(String engagementIdCode64Prefix);

    List<InvoiceEntity> findByEngagementIdCode64StartingWithAndEmissionDateGreaterThanEqualOrderByEmissionDateDesc(
            String engagementIdCode64Prefix, LocalDate emissionDate);

    Optional<InvoiceEntity> findFirstBySeriesOrderByNumberDesc(String series);
}
