package es.upm.api.adapter.out.billing.mongo.invoice;

import es.upm.api.domain.model.InvoicedPayment;
import es.upm.api.domain.model.PaymentMethod;
import es.upm.api.domain.model.external.UserSnapshot;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoicedPaymentEntity {
    private String paymentId;
    private LocalDate date;
    private BigDecimal amount;
    private PaymentMethod method;
    private UserSnapshot user;

    public InvoicedPaymentEntity(InvoicedPayment invoicedPayment) {
        BeanUtils.copyProperties(invoicedPayment, this);
        this.paymentId = invoicedPayment.paymentId() == null ? null : invoicedPayment.paymentId().toString();
    }

    public InvoicedPayment toDomain() {
        return new InvoicedPayment(
                this.getPaymentId() == null ? null : UUID.fromString(this.getPaymentId()),
                this.getDate(),
                this.getAmount(),
                this.getMethod(),
                this.getUser()
        );
    }
}
