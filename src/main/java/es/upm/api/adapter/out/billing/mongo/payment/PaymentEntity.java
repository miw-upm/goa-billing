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
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document
public class PaymentEntity {
    @Id
    private String id;
    private String engagementId;
    private String engagementIdCode64;
    private String userId;
    private BigDecimal amount;
    private PaymentMethod method;
    private LocalDate date;
    private Boolean invoiced;

    public PaymentEntity(Payment payment) {
        BeanUtils.copyProperties(payment, this);
        this.id = payment.getId() == null ? null : payment.getId().toString();
        this.engagementId = payment.getEngagement() == null || payment.getEngagement().getId() == null
                ? null : payment.getEngagement().getId().toString();
        this.engagementIdCode64 = this.engagementId == null ? null : PaymentEntity.encodeEngagementId(this.engagementId);
        this.userId = payment.getUser() == null || payment.getUser().getId() == null
                ? null : payment.getUser().getId().toString();
    }

    public Payment toDomain() {
        Payment payment = new Payment();
        BeanUtils.copyProperties(this, payment);
        payment.setId(this.id == null ? null : UUID.fromString(this.id));
        payment.setEngagement(this.engagementId == null ? null
                : EngagementSnapshot.builder().id(UUID.fromString(this.engagementId)).build());
        payment.setUser(this.userId == null ? null
                : UserSnapshot.builder().id(UUID.fromString(this.userId)).build());
        return payment;
    }

    public static String encodeEngagementId(String engagementId) {
        UUID uuid = UUID.fromString(engagementId);
        ByteBuffer byteBuffer = ByteBuffer.allocate(16);
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());
        return Base64.getEncoder().withoutPadding().encodeToString(byteBuffer.array());
    }
}
