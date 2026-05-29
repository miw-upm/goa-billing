package es.upm.api.adapter.out.billing.mongo.invoice;

import es.upm.api.adapter.out.billing.mongo.expense.ExpenseEntity;
import es.upm.api.adapter.out.billing.mongo.payment.PaymentEntity;
import es.upm.api.domain.model.*;
import es.upm.api.domain.model.external.EngagementSnapshot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        this.id = invoice.getId() == null ? null : invoice.getId().toString();
        this.billingInfo = invoice.getBillingInfo() == null ? null : new BillingInfoEntity(invoice.getBillingInfo());
        this.engagementId = invoice.getEngagement() == null || invoice.getEngagement().getId() == null
                ? null : invoice.getEngagement().getId().toString();
        this.legalProcedures = invoice.getLegalProcedures() == null ? null
                : invoice.getLegalProcedures().stream().map(LegalProcedureEntity::new).toList();
        this.payments = invoice.getPayments() == null ? null
                : invoice.getPayments().stream().map(InvoiceEntity::toPaymentEntity).toList();
        this.priorPayments = invoice.getPriorPayments() == null ? null
                : invoice.getPriorPayments().stream().map(InvoiceEntity::toPaymentEntity).toList();
        this.expenses = invoice.getExpenses() == null ? null
                : invoice.getExpenses().stream().map(InvoiceEntity::toExpenseEntity).toList();
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
                : this.payments.stream().map(paymentEntity -> new InvoicedPayment(paymentEntity.toDomain())).toList());
        invoice.setPriorPayments(this.priorPayments == null ? null
                : this.priorPayments.stream().map(paymentEntity -> new InvoicedPayment(paymentEntity.toDomain())).toList());
        invoice.setExpenses(this.expenses == null ? null
                : this.expenses.stream().map(InvoiceEntity::toInvoicedExpense).toList());
        return invoice;
    }

    private static PaymentEntity toPaymentEntity(InvoicedPayment invoicedPayment) {
        Payment payment = Payment.builder()
                .id(invoicedPayment.paymentId())
                .date(invoicedPayment.date())
                .amount(invoicedPayment.amount())
                .method(invoicedPayment.method())
                .user(invoicedPayment.user())
                .build();
        return new PaymentEntity(payment);
    }

    private static ExpenseEntity toExpenseEntity(InvoicedExpense invoicedExpense) {
        Integer vatRate = null;
        if (invoicedExpense.baseAmount() != null
                && invoicedExpense.vatAmount() != null
                && invoicedExpense.baseAmount().compareTo(BigDecimal.ZERO) != 0) {
            vatRate = invoicedExpense.vatAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(invoicedExpense.baseAmount(), 0, RoundingMode.HALF_UP)
                    .intValue();
        }
        Expense expense = Expense.builder()
                .id(invoicedExpense.expenseId())
                .issueDate(invoicedExpense.issueDate())
                .description(invoicedExpense.description())
                .baseAmount(invoicedExpense.baseAmount())
                .vatRate(vatRate)
                .build();
        return new ExpenseEntity(expense);
    }

    private static InvoicedExpense toInvoicedExpense(ExpenseEntity expenseEntity) {
        BigDecimal vatAmount = BigDecimal.ZERO;
        if (expenseEntity.getBaseAmount() != null && expenseEntity.getVatRate() != null) {
            vatAmount = expenseEntity.getBaseAmount()
                    .multiply(BigDecimal.valueOf(expenseEntity.getVatRate()))
                    .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        }
        return new InvoicedExpense(
                expenseEntity.getId() == null ? null : UUID.fromString(expenseEntity.getId()),
                expenseEntity.getIssueDate(),
                expenseEntity.getDescription(),
                expenseEntity.getBaseAmount(),
                vatAmount
        );
    }
}
