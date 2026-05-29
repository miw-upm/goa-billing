package es.upm.api.adapter.out.billing.mongo.invoice;

import es.upm.api.domain.model.*;
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
    private String id;
    private String concept;
    private Boolean closed;
    private BillingInfoEntity billingInfo;
    private BigDecimal percentage;
    private LocalDate emissionDate;
    private LocalDate operationDate;
    private String series;
    private Integer number;
    private BigDecimal baseAmount;
    private BigDecimal vatAmount;
    private BigDecimal vatRate;
    private String engagementId;
    private List<InvoiceLegalProcedureEntity> legalProcedures;
    private List<InvoicedPaymentEntity> payments;
    private List<InvoicedPaymentEntity> priorPayments;
    private List<InvoicedExpenseEntity> expenses;
    private List<BigDecimal> discounts;
    private String pdfPath;
    private Rectification rectification;

    public InvoiceEntity(Invoice invoice) {
        BeanUtils.copyProperties(invoice, this);
        this.id = invoice.getId() == null ? null : invoice.getId().toString();
        this.billingInfo = invoice.getBillingInfo() == null ? null : new BillingInfoEntity(invoice.getBillingInfo());
        this.engagementId = invoice.getEngagement() == null || invoice.getEngagement().getId() == null
                ? null : invoice.getEngagement().getId().toString();
        this.legalProcedures = invoice.getLegalProcedures() == null ? null
                : invoice.getLegalProcedures().stream().map(InvoiceLegalProcedureEntity::new).toList();
        this.payments = invoice.getPayments() == null ? null
                : invoice.getPayments().stream().map(InvoicedPaymentEntity::new).toList();
        this.priorPayments = invoice.getPriorPayments() == null ? null
                : invoice.getPriorPayments().stream().map(InvoicedPaymentEntity::new).toList();
        this.expenses = invoice.getExpenses() == null ? null
                : invoice.getExpenses().stream().map(InvoicedExpenseEntity::new).toList();
    }

    public Invoice toDomain() {
        Invoice invoice = new Invoice();
        BeanUtils.copyProperties(this, invoice);
        invoice.setId(this.id == null ? null : UUID.fromString(this.id));
        invoice.setBillingInfo(this.billingInfo == null ? null : this.billingInfo.toDomain());
        invoice.setEngagement(this.engagementId == null ? null
                : EngagementSnapshot.builder().id(UUID.fromString(this.engagementId)).build());
        invoice.setLegalProcedures(this.legalProcedures == null ? null
                : this.legalProcedures.stream().map(InvoiceLegalProcedureEntity::toDomain).toList());
        invoice.setPayments(this.payments == null ? null
                : this.payments.stream().map(InvoicedPaymentEntity::toDomain).toList());
        invoice.setPriorPayments(this.priorPayments == null ? null
                : this.priorPayments.stream().map(InvoicedPaymentEntity::toDomain).toList());
        invoice.setExpenses(this.expenses == null ? null
                : this.expenses.stream().map(InvoicedExpenseEntity::toDomain).toList());
        return invoice;
    }
}
