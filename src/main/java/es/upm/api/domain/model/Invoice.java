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
        BigDecimal divisor = BigDecimal.ONE.add(
                vatRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        return sum(priorPayments, p -> p.amount().divide(divisor, 4, RoundingMode.HALF_UP));
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
        this.vatAmount = baseAmount
                .multiply(vatRate)
                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
    }

    public void applyBaseAmount(BigDecimal totalAmount) {
        BigDecimal divisor = BigDecimal.ONE.add(
                this.vatRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        this.baseAmount = totalAmount.divide(divisor, 4, RoundingMode.HALF_UP);
        this.vatAmount = baseAmount
                .multiply(vatRate)
                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
    }

    public BigDecimal percentageFactor(){
        return this.percentage.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
    }

    public BigDecimal totalBaseAmount() {
        return baseAmount
                .subtract(priorPaymentsBaseAmount())
                .subtract(discountsAmount())
                .add(expensesBaseAmount())
                .multiply(this.percentageFactor());
    }

    public BigDecimal totalVatAmount() {
        return vatAmount
                .subtract(priorPaymentsVatAmount())
                .add(expensesVatAmount())
                .multiply(this.percentageFactor());
    }
}
