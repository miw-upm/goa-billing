package es.upm.api.adapter.out.billing.mongo.payment;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends MongoRepository<PaymentEntity, UUID> {
    @Query("{'date': ?0}")
    List<PaymentEntity> findByDate(LocalDate date);

    @Query("{'engagementId': ?0}")
    List<PaymentEntity> findByEngagementId(UUID engagementId);
}
