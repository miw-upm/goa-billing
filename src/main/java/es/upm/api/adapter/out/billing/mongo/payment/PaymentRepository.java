package es.upm.api.adapter.out.billing.mongo.payment;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends MongoRepository<PaymentEntity, UUID> {
    List<PaymentEntity> findByDateGreaterThanEqualOrderByDateDesc(LocalDate fromDate);
}
