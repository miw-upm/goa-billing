package es.upm.api.adapter.out.billing.mongo.payment;

import es.upm.api.domain.model.Payment;
import es.upm.api.domain.model.criteria.PaymentFindCriteria;
import es.upm.api.domain.ports.out.billing.PaymentGateway;
import es.upm.miw.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
@RequiredArgsConstructor
public class PaymentAdapter implements PaymentGateway {
    private final PaymentRepository paymentRepository;

    @Override
    public void create(Payment payment) {
        this.paymentRepository.save(new PaymentEntity(payment));
    }

    @Override
    public Payment update(UUID id, Payment payment) {
        PaymentEntity paymentEntity = this.paymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payment id: " + id));

        paymentEntity.setEngagementId(payment.getEngagement().getId());
        paymentEntity.setUserId(payment.getUser().getId());
        paymentEntity.setAmount(payment.getAmount());
        paymentEntity.setMethod(payment.getMethod());
        paymentEntity.setDate(payment.getDate());
        paymentEntity.setInvoiced(payment.getInvoiced());

        return this.paymentRepository.save(paymentEntity).toDomain();
    }

    @Override
    public Payment read(UUID id) {
        return this.paymentRepository.findById(id)
                .map(PaymentEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("Payment id: " + id));
    }

    @Override
    public void delete(UUID id) {
        PaymentEntity paymentEntity = this.paymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payment id: " + id));
        this.paymentRepository.delete(paymentEntity);
    }

    @Override
    public Stream<Payment> find(PaymentFindCriteria criteria) {
        List<PaymentEntity> result;
        if (criteria.all()) {
            result = this.paymentRepository.findAllByOrderByDateDesc();
        } else if (criteria.getFromDate() != null && criteria.getInvoiced() != null) {
            result = this.paymentRepository.findByDateGreaterThanEqualAndInvoicedOrderByDateDesc(
                    criteria.getFromDate(), criteria.getInvoiced()
            );
        } else if (criteria.getFromDate() != null) {
            result = this.paymentRepository.findByDateGreaterThanEqualOrderByDateDesc(criteria.getFromDate());
        } else if (criteria.getInvoiced() != null) {
            result = this.paymentRepository.findByInvoicedOrderByDateDesc(criteria.getInvoiced());
        } else {
            result = this.paymentRepository.findAllByOrderByDateDesc();
        }

        return result.stream()
                .map(PaymentEntity::toDomain);
    }

    @Override
    public Stream<Payment> findNotInvoicedByEngagementId(UUID engagementId) {
        return this.paymentRepository.findByEngagementIdAndInvoicedFalseOrderByDateDesc(engagementId).stream()
                .map(PaymentEntity::toDomain);
    }
}
