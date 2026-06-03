package es.upm.api.domain.services;

import es.upm.api.domain.model.Payment;
import es.upm.api.domain.model.criteria.PaymentFindCriteria;
import es.upm.api.domain.model.external.UserSnapshot;
import es.upm.api.domain.ports.out.billing.PaymentGateway;
import es.upm.api.domain.ports.out.engagement.EngagementGateway;
import es.upm.api.domain.ports.out.user.UserFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentGateway paymentGateway;
    private final EngagementGateway engagementGateway;
    private final UserFinder userFinder;

    public void create(Payment payment) {
        payment.setId(UUID.randomUUID());
        payment.setInvoiced(false);
        this.hydrateEngagement(payment);
        payment.setUser(this.userFinder.readById(payment.getUser().getId()));
        this.paymentGateway.create(payment);
    }

    public Payment read(UUID id) {
        Payment payment = this.paymentGateway.read(id);
        this.hydrateEngagement(payment);
        payment.setUser(this.userFinder.readById(payment.getUser().getId()));
        return payment;
    }

    public void update(UUID id, Payment payment) {
        Payment currentPayment = this.paymentGateway.read(id);
        payment.setId(id);
        payment.setDate(currentPayment.getDate());
        if (Objects.isNull(payment.getInvoiced())) {
            payment.setInvoiced(currentPayment.getInvoiced());
        }
        this.hydrateEngagement(payment);
        payment.setUser(this.userFinder.readById(payment.getUser().getId()));
        this.paymentGateway.update(id, payment);
    }

    public void delete(UUID id) {
        this.paymentGateway.delete(id);
    }

    public Stream<Payment> find(PaymentFindCriteria criteria) {
        Stream<Payment> payments = this.paymentGateway.find(criteria);
        if (criteria == null || !StringUtils.hasText(criteria.getClient())) {
            return payments.map(this::hydrate);
        }
        List<UUID> clientIds = this.userFinder.find(criteria.getClient()).stream()
                .map(UserSnapshot::getId)
                .toList();
        return payments.filter(payment -> payment.getUser() != null
                        && payment.getUser().getId() != null
                        && clientIds.contains(payment.getUser().getId()))
                .map(this::hydrate);
    }

    private Payment hydrate(Payment payment) {
        this.hydrateEngagement(payment);
        payment.setUser(this.userFinder.readById(payment.getUser().getId()));
        return payment;
    }

    private void hydrateEngagement(Payment payment) {
        if (payment.getEngagement() != null && payment.getEngagement().getId() != null) {
            payment.setEngagement(this.engagementGateway.read(payment.getEngagement().getId()));
        }
    }
}
