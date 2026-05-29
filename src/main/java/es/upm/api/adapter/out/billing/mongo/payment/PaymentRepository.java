package es.upm.api.adapter.out.billing.mongo.payment;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface PaymentRepository extends MongoRepository<PaymentEntity, String> {
    List<PaymentEntity> findAllByOrderByDateDesc();

    List<PaymentEntity> findByDateGreaterThanEqualOrderByDateDesc(LocalDate fromDate);

    List<PaymentEntity> findByInvoicedOrderByDateDesc(Boolean invoiced);

    List<PaymentEntity> findByDateGreaterThanEqualAndInvoicedOrderByDateDesc(LocalDate fromDate, Boolean invoiced);

    List<PaymentEntity> findByEngagementIdStartingWithOrderByDateDesc(String engagementIdPrefix);

    List<PaymentEntity> findByEngagementIdStartingWithAndDateGreaterThanEqualOrderByDateDesc(
            String engagementIdPrefix, LocalDate fromDate
    );

    List<PaymentEntity> findByEngagementIdStartingWithAndInvoicedOrderByDateDesc(
            String engagementIdPrefix, Boolean invoiced
    );

    List<PaymentEntity> findByEngagementIdStartingWithAndDateGreaterThanEqualAndInvoicedOrderByDateDesc(
            String engagementIdPrefix, LocalDate fromDate, Boolean invoiced
    );

    List<PaymentEntity> findByEngagementIdAndInvoicedFalseOrderByDateDesc(String engagementId);

    List<PaymentEntity> findByEngagementIdAndInvoicedTrueOrderByDateDesc(String engagementId);
}
