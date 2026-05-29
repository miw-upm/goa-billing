package es.upm.api.adapter.out.billing.mongo.payment;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface PaymentRepository extends MongoRepository<PaymentEntity, String> {
    List<PaymentEntity> findAllByOrderByDateDesc();

    List<PaymentEntity> findByDateGreaterThanEqualOrderByDateDesc(LocalDate fromDate);

    List<PaymentEntity> findByInvoicedOrderByDateDesc(Boolean invoiced);

    List<PaymentEntity> findByDateGreaterThanEqualAndInvoicedOrderByDateDesc(LocalDate fromDate, Boolean invoiced);

    List<PaymentEntity> findByEngagementIdCode64StartingWithOrderByDateDesc(String engagementIdCode64Prefix);

    List<PaymentEntity> findByEngagementIdCode64StartingWithAndDateGreaterThanEqualOrderByDateDesc(
            String engagementIdCode64Prefix, LocalDate fromDate
    );

    List<PaymentEntity> findByEngagementIdCode64StartingWithAndInvoicedOrderByDateDesc(
            String engagementIdCode64Prefix, Boolean invoiced
    );

    List<PaymentEntity> findByEngagementIdCode64StartingWithAndDateGreaterThanEqualAndInvoicedOrderByDateDesc(
            String engagementIdCode64Prefix, LocalDate fromDate, Boolean invoiced
    );

    List<PaymentEntity> findByEngagementIdAndInvoicedFalseOrderByDateDesc(String engagementId);

    List<PaymentEntity> findByEngagementIdAndInvoicedTrueOrderByDateDesc(String engagementId);
}
