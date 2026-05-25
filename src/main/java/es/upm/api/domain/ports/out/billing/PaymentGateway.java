package es.upm.api.domain.ports.out.billing;

import es.upm.api.domain.model.Payment;
import es.upm.api.domain.model.criteria.PaymentFindCriteria;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.stream.Stream;

@Repository
public interface PaymentGateway {
    void create(Payment payment);

    Payment update(UUID id, Payment payment);

    Payment read(UUID id);

    void delete(UUID id);

    Stream<Payment> find(PaymentFindCriteria criteria);

    Stream<Payment> findNotInvoicedByEngagementId(UUID engagementId);
}
