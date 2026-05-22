package es.upm.api.adapter.out.billing.mongo.invoice;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InvoiceRepository extends MongoRepository<InvoiceEntity, UUID> {
    @Query("{'engagementId': ?0}")
    List<InvoiceEntity> findByEngagementId(UUID engagementId);

    @Query("{'emissionDate': ?0}")
    List<InvoiceEntity> findByEmissionDate(LocalDate emissionDate);
}
