package es.upm.api.adapter.out.billing.mongo.invoice;

import es.upm.api.domain.model.*;
import es.upm.api.domain.model.creation.InvoiceLegalProcedure;
import es.upm.api.domain.model.external.EngagementSnapshot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.nio.ByteBuffer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document
public class InvoiceEntity {
    @Id
    private String id;
    private String concept;
    private Boolean closed;
    private BillingInfo billingInfo;
    private BigDecimal percentage;
    private LocalDate emissionDate;
    private LocalDate operationDate;
    private String series;
    private Integer number;
    private BigDecimal baseAmount;
    private BigDecimal vatAmount;
    private BigDecimal vatRate;
    private String engagementId;
    private String engagementIdCode64;
    private List<InvoiceLegalProcedure> legalProcedures;
    private List<InvoicedPayment> payments;
    private List<InvoicedPayment> priorPayments;
    private List<InvoicedExpense> expenses;
    private List<BigDecimal> discounts;
    private String pdfPath;
    private Rectification rectification;

    public InvoiceEntity(Invoice invoice) {
        BeanUtils.copyProperties(invoice, this);
        this.id = invoice.getId() == null ? null : invoice.getId().toString();
        this.engagementId = invoice.getEngagement() == null || invoice.getEngagement().getId() == null
                ? null : invoice.getEngagement().getId().toString();
        this.engagementIdCode64 = this.engagementId == null ? null : InvoiceEntity.encodeEngagementId(this.engagementId);
    }

    public Invoice toDomain() {
        Invoice invoice = new Invoice();
        BeanUtils.copyProperties(this, invoice);
        invoice.setId(this.id == null ? null : UUID.fromString(this.id));
        invoice.setEngagement(this.engagementId == null ? null
                : EngagementSnapshot.builder().id(UUID.fromString(this.engagementId)).build());
        return invoice;
    }

    public static String encodeEngagementId(String engagementId) {
        UUID uuid = UUID.fromString(engagementId);
        ByteBuffer byteBuffer = ByteBuffer.allocate(16);
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());
        return Base64.getEncoder().withoutPadding().encodeToString(byteBuffer.array());
    }
}
