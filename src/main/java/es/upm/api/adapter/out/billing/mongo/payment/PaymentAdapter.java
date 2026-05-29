package es.upm.api.adapter.out.billing.mongo.payment;

import es.upm.api.domain.model.Payment;
import es.upm.api.domain.model.criteria.PaymentFindCriteria;
import es.upm.api.domain.ports.out.billing.PaymentGateway;
import es.upm.miw.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

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
        PaymentEntity paymentEntity = this.paymentRepository.findById(id.toString())
                .orElseThrow(() -> new NotFoundException("Payment id: " + id));

        paymentEntity.setEngagementId(payment.getEngagement() == null || payment.getEngagement().getId() == null
                ? null : payment.getEngagement().getId().toString());
        paymentEntity.setUserId(payment.getUser() == null || payment.getUser().getId() == null
                ? null : payment.getUser().getId().toString());
        paymentEntity.setAmount(payment.getAmount());
        paymentEntity.setMethod(payment.getMethod());
        paymentEntity.setDate(payment.getDate());
        paymentEntity.setInvoiced(payment.getInvoiced());

        return this.paymentRepository.save(paymentEntity).toDomain();
    }

    @Override
    public Payment read(UUID id) {
        return this.paymentRepository.findById(id.toString())
                .map(PaymentEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("Payment id: " + id));
    }

    @Override
    public void delete(UUID id) {
        PaymentEntity paymentEntity = this.paymentRepository.findById(id.toString())
                .orElseThrow(() -> new NotFoundException("Payment id: " + id));
        this.paymentRepository.delete(paymentEntity);
    }

    @Override
    public Stream<Payment> find(PaymentFindCriteria criteria) {
        List<PaymentEntity> result;
        if (StringUtils.hasText(criteria.getEngagementId()) && criteria.getFromDate() != null && criteria.getInvoiced() != null) {
            result = this.paymentRepository.findByEngagementIdStartingWithAndDateGreaterThanEqualAndInvoicedOrderByDateDesc(
                    this.normalizeEngagementIdPrefix(criteria.getEngagementId()), criteria.getFromDate(), criteria.getInvoiced()
            );
        } else if (StringUtils.hasText(criteria.getEngagementId()) && criteria.getFromDate() != null) {
            result = this.paymentRepository.findByEngagementIdStartingWithAndDateGreaterThanEqualOrderByDateDesc(
                    this.normalizeEngagementIdPrefix(criteria.getEngagementId()), criteria.getFromDate()
            );
        } else if (StringUtils.hasText(criteria.getEngagementId()) && criteria.getInvoiced() != null) {
            result = this.paymentRepository.findByEngagementIdStartingWithAndInvoicedOrderByDateDesc(
                    this.normalizeEngagementIdPrefix(criteria.getEngagementId()), criteria.getInvoiced()
            );
        } else if (StringUtils.hasText(criteria.getEngagementId())) {
            result = this.paymentRepository.findByEngagementIdStartingWithOrderByDateDesc(
                    this.normalizeEngagementIdPrefix(criteria.getEngagementId())
            );
        } else if (criteria.all()) {
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

    private String normalizeEngagementIdPrefix(String engagementId) {
        String normalized = engagementId.trim();
        return normalized.length() <= 4 ? normalized : normalized.substring(0, 4);
    }

    @Override
    public Stream<Payment> findNotInvoicedByEngagementId(UUID engagementId) {
        return this.paymentRepository.findByEngagementIdAndInvoicedFalseOrderByDateDesc(engagementId.toString()).stream()
                .map(PaymentEntity::toDomain);
    }

    @Override
    public Stream<Payment> findInvoicedByEngagementId(UUID engagementId) {
        return this.paymentRepository.findByEngagementIdAndInvoicedTrueOrderByDateDesc(engagementId.toString()).stream()
                .map(PaymentEntity::toDomain);
    }
}
