package es.upm.api.adapter.out.billing.mongo.payment;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends MongoRepository<PaymentEntity, UUID> {
    List<PaymentEntity> findAllByOrderByDateDesc();

    List<PaymentEntity> findByDateGreaterThanEqualOrderByDateDesc(LocalDate fromDate);

    List<PaymentEntity> findByInvoicedOrderByDateDesc(Boolean invoiced);

    List<PaymentEntity> findByDateGreaterThanEqualAndInvoicedOrderByDateDesc(LocalDate fromDate, Boolean invoiced);

    List<PaymentEntity> findByEngagementIdAndInvoicedFalseOrderByDateDesc(UUID engagementId);

    List<PaymentEntity> findByEngagementIdAndInvoicedTrueOrderByDateDesc(UUID engagementId);
}
