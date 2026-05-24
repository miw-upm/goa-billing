package es.upm.api.adapter.out.billing.mongo.invoice;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InvoiceRepository extends MongoRepository<InvoiceEntity, UUID> {
    List<InvoiceEntity> findAllByOrderByEmissionDateDesc();

    List<InvoiceEntity> findByEmissionDateGreaterThanEqualOrderByEmissionDateDesc(LocalDate emissionDate);
}
