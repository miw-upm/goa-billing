package es.upm.api.domain.services;

import es.upm.api.domain.model.Payment;
import es.upm.api.domain.model.criteria.PaymentFindCriteria;
import es.upm.api.domain.ports.out.billing.PaymentGateway;
import es.upm.api.domain.ports.out.engagement.EngagementFinder;
import es.upm.api.domain.ports.out.user.UserFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentGateway paymentGateway;
    private final EngagementFinder engagementFinder;
    private final UserFinder userFinder;

    public Payment create(Payment payment) {
        payment.setId(UUID.randomUUID());
        payment.setDate(LocalDate.now());
        payment.setEngagement(this.engagementFinder.read(payment.getEngagement().getEngagementId()));
        payment.setUser(this.userFinder.readById(payment.getUser().getId()));
        this.paymentGateway.create(payment);
        return payment;
    }

    public Payment read(UUID id) {
        Payment payment = this.paymentGateway.read(id);
        payment.setEngagement(this.engagementFinder.read(payment.getEngagement().getEngagementId()));
        payment.setUser(this.userFinder.readById(payment.getUser().getId()));
        return payment;
    }

    public Payment update(UUID id, Payment payment) {
        Payment currentPayment = this.paymentGateway.read(id);
        payment.setId(id);
        payment.setDate(currentPayment.getDate());
        payment.setEngagement(this.engagementFinder.read(payment.getEngagement().getEngagementId()));
        payment.setUser(this.userFinder.readById(payment.getUser().getId()));
        return this.paymentGateway.update(id, payment);
    }

    public void delete(UUID id) {
        this.paymentGateway.delete(id);
    }

    public Stream<Payment> find(PaymentFindCriteria criteria) {
        return this.paymentGateway.find(criteria);
    }
}
