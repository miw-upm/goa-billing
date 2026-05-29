package es.upm.api.adapter.out.billing.mongo.invoice;

import es.upm.api.adapter.out.billing.mongo.expense.ExpenseEntity;
import es.upm.api.adapter.out.billing.mongo.payment.PaymentEntity;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.Rectification;
import es.upm.api.domain.model.external.EngagementSnapshot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

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
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal percentage;
    private LocalDate emissionDate;
    private LocalDate operationDate;
    private String series;
    private Integer number;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal baseAmount;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal vatAmount;
    private BigDecimal vatRate;
    private String engagementId;
    private List<LegalProcedureEntity> legalProcedures;
    @DBRef
    private List<PaymentEntity> payments;
    @DBRef
    private List<PaymentEntity> priorPayments;
    @DBRef
    private List<ExpenseEntity> expenses;
    private List<BigDecimal> discounts;
    private String pdfPath;
    private Rectification rectification;

    public InvoiceEntity(Invoice invoice) {
        BeanUtils.copyProperties(invoice, this);
        this.id = invoice.getId().toString();
        this.billingInfo = new BillingInfoEntity(invoice.getBillingInfo());
        this.engagementId = invoice.getEngagement() == null || invoice.getEngagement().getId() == null
                ? null : invoice.getEngagement().getId().toString();
        this.legalProcedures = invoice.getLegalProcedures() == null ? null
                : invoice.getLegalProcedures().stream().map(LegalProcedureEntity::new).toList();
        this.payments = invoice.getPayments() == null ? null
                : invoice.getPayments().stream().map(PaymentEntity::new).toList();
        this.priorPayments = invoice.getPriorPayments() == null ? null
                : invoice.getPriorPayments().stream().map(PaymentEntity::new).toList();
        this.expenses = invoice.getExpenses() == null ? null
                : invoice.getExpenses().stream().map(ExpenseEntity::new).toList();
    }

    public Invoice toDomain() {
        Invoice invoice = new Invoice();
        BeanUtils.copyProperties(this, invoice);
        invoice.setId(this.id == null ? null : UUID.fromString(this.id));
        invoice.setBillingInfo(this.billingInfo == null ? null : this.billingInfo.toDomain());
        invoice.setEngagement(this.engagementId == null ? null
                : EngagementSnapshot.builder().id(UUID.fromString(this.engagementId)).build());
        invoice.setLegalProcedures(this.legalProcedures == null ? null
                : this.legalProcedures.stream().map(LegalProcedureEntity::toDomain).toList());
        invoice.setPayments(this.payments == null ? null
                : this.payments.stream().map(PaymentEntity::toDomain).toList());
        invoice.setPriorPayments(this.priorPayments == null ? null
                : this.priorPayments.stream().map(PaymentEntity::toDomain).toList());
        invoice.setExpenses(this.expenses == null ? null
                : this.expenses.stream().map(ExpenseEntity::toDomain).toList());
        return invoice;
    }
}
