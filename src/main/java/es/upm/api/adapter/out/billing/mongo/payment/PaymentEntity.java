package es.upm.api.adapter.out.billing.mongo.payment;

import es.upm.api.domain.model.Payment;
import es.upm.api.domain.model.PaymentMethod;
import es.upm.api.domain.model.external.EngagementSnapshot;
import es.upm.api.domain.model.external.UserSnapshot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document
public class PaymentEntity {
    @Id
    private UUID id;
    private UUID engagementId;
    private UUID userId;
    private BigDecimal amount;
    private PaymentMethod method;
    private LocalDate date;
    private Boolean invoiced;

    public PaymentEntity(Payment payment) {
        BeanUtils.copyProperties(payment, this);
        this.engagementId = payment.getEngagement() == null ? null : payment.getEngagement().getId();
        this.userId = payment.getUser() == null ? null : payment.getUser().getId();
    }

    public Payment toDomain() {
        Payment payment = new Payment();
        BeanUtils.copyProperties(this, payment);
        payment.setEngagement(this.engagementId == null ? null
                : EngagementSnapshot.builder().id(this.engagementId).build());
        payment.setUser(this.userId == null ? null
                : UserSnapshot.builder().id(this.userId).build());
        return payment;
    }
}

