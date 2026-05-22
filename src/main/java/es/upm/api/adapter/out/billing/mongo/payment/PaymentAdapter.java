package es.upm.api.adapter.out.billing.mongo.payment;

import es.upm.api.domain.model.Payment;
import es.upm.api.domain.model.criteria.PaymentFindCriteria;
import es.upm.api.domain.ports.out.billing.PaymentGateway;
import es.upm.miw.exception.NotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
public class PaymentAdapter implements PaymentGateway {
    public static final Sort DATE = Sort.by(Sort.Direction.DESC, "date");

    private final PaymentRepository paymentRepository;

    public PaymentAdapter(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public void create(Payment payment) {
        this.paymentRepository.save(new PaymentEntity(payment));
    }

    @Override
    public Payment update(UUID id, Payment payment) {
        PaymentEntity paymentEntity = this.paymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payment id: " + id));

        paymentEntity.setEngagementId(payment.getEngagement().getEngagementId());
        paymentEntity.setUserId(payment.getUser().getId());
        paymentEntity.setAmount(payment.getAmount());
        paymentEntity.setMethod(payment.getMethod());
        paymentEntity.setDate(payment.getDate());

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
        List<PaymentEntity> result = (criteria != null && criteria.getFromDate() != null)
                ? this.paymentRepository.findByDateGreaterThanEqualOrderByDateDesc(criteria.getFromDate())
                : this.paymentRepository.findAll(DATE);

        return result.stream()
                .map(PaymentEntity::toDomain);
    }
}
