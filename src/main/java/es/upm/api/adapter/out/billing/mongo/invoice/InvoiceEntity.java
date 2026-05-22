package es.upm.api.adapter.out.billing.mongo.invoice;

import es.upm.api.domain.model.BillingInfo;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.Payment;
import es.upm.api.domain.model.Rectification;
import es.upm.api.domain.model.external.EngagementSnapshot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document
public class InvoiceEntity {
    @Id
    private UUID id;
    private BillingInfo billingInfo;
    private LocalDate emissionDate;
    private LocalDate operationDate;
    private String series;
    private Integer number;
    private BigDecimal baseAmount;
    private BigDecimal vatRate;
    private UUID engagementId;
    private List<Payment> payments;
    private List<BigDecimal> discounts;
    private String pdfPath;
    private Rectification rectification;

    public InvoiceEntity(Invoice invoice) {
        BeanUtils.copyProperties(invoice, this);
        this.engagementId = invoice.getEngagement() == null ? null : invoice.getEngagement().getId();
    }

    public Invoice toDomain() {
        Invoice invoice = new Invoice();
        BeanUtils.copyProperties(this, invoice);
        invoice.setEngagement(this.engagementId == null ? null
                : EngagementSnapshot.builder().id(this.engagementId).build());
        return invoice;
    }
}
