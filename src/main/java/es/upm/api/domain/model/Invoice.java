package es.upm.api.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import es.upm.api.domain.model.external.EngagementSnapshot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    private UUID id;
    private BillingInfo billingInfo;
    private BigDecimal percentage;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate emissionDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate operationDate;
    private String series;
    private Integer number;
    private BigDecimal baseAmount;
    private BigDecimal vatAmount;
    private BigDecimal vatRate;
    private EngagementSnapshot engagement;
    private List<InvoicedPayment> payments;
    private List<InvoicedPayment> priorPayments;
    private List<InvoicedExpense> expenses;
    private List<BigDecimal> discounts;
    private Boolean closed;
    private String pdfPath;
    private Rectification rectification;

    public boolean isIssued() {
        return this.emissionDate != null;
    }

    private <T> BigDecimal sum(List<T> list, Function<T, BigDecimal> mapper) {
        return Optional.ofNullable(list)
                .orElse(List.of())
                .stream()
                .map(mapper)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal paymentsAmount() {
        return sum(payments, InvoicedPayment::amount);
    }

    public BigDecimal expensesBaseAmount() {
        return sum(expenses, InvoicedExpense::baseAmount);
    }

    public BigDecimal expensesVatAmount() {
        return sum(expenses, InvoicedExpense::vatAmount);
    }

    public BigDecimal priorPaymentsAmount() {
        return sum(priorPayments, InvoicedPayment::amount);
    }

    public BigDecimal priorPaymentsBaseAmount() {
        return this.sum(this.priorPayments, payment -> payment.amount().multiply(this.baseShare()));
    }

    public BigDecimal priorPaymentsVatAmount() {
        return priorPaymentsAmount().subtract(priorPaymentsBaseAmount());
    }

    public BigDecimal discountsAmount() {
        return sum(discounts, Function.identity());
    }

    public BigDecimal totalAmount() {
        return baseAmount
                .add(vatAmount)
                .add(expensesBaseAmount())
                .add(expensesVatAmount());
    }

    public void applyVatRate(BigDecimal vatRate) {
        this.vatRate = vatRate;
        this.vatAmount = this.baseAmount.multiply(this.vatFactor());
    }

    public void applyBaseAmount(BigDecimal baseAmount) {
        this.baseAmount = baseAmount;
        this.vatAmount = this.baseAmount.multiply(this.vatFactor());
    }

    public void applyTotalAmount(BigDecimal totalAmount) {
        this.baseAmount = totalAmount.multiply(this.baseShare());
        this.vatAmount = this.baseAmount.multiply(this.vatFactor());
    }

    public BigDecimal percentageFactor(){
        return this.percentage.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
    }

    public BigDecimal totalBaseAmount() {
        return baseAmount
                .subtract(this.priorPaymentsBaseAmount())
                .subtract(this.discountsAmount())
                .add(this.expensesBaseAmount())
                .multiply(this.percentageFactor());
    }

    public BigDecimal totalVatAmount() {
        return vatAmount
                .subtract(this.priorPaymentsVatAmount())
                .add(this.expensesVatAmount())
                .multiply(this.percentageFactor());
    }

    private BigDecimal vatFactor() {
        return vatRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal vatTotal() {
        return BigDecimal.ONE.add(vatFactor());
    }

    private BigDecimal vatShare() {
        return vatFactor().divide(vatTotal(), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal baseShare() {
        return BigDecimal.ONE.divide(vatTotal(), 4, RoundingMode.HALF_UP);
    }

}
