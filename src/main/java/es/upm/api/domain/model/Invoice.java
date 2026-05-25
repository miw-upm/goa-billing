package es.upm.api.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import es.upm.api.domain.model.external.EngagementSnapshot;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UUID id;

    @NotNull
    private BillingInfo billingInfo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDate emissionDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @PastOrPresent
    private LocalDate operationDate;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String series;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer number;

    @NotNull
    @Positive
    private BigDecimal baseAmount;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal vatRate;

    private EngagementSnapshot engagement;
    private List<Payment> payments;
    private List<Payment> invoicedPayments;
    private List<Expense> expenses;
    private List<BigDecimal> discounts;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String pdfPath;

    private Rectification rectification;

    public boolean isIssued() {
        return this.emissionDate != null;
    }

    public BigDecimal getServiceVatAmount() {
        BigDecimal effectiveVatRate = this.vatRate == null ? BigDecimal.ZERO : this.vatRate;
        BigDecimal effectiveBaseAmount = this.baseAmount == null ? BigDecimal.ZERO : this.baseAmount;
        return effectiveBaseAmount.multiply(effectiveVatRate)
                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
    }

    public BigDecimal getExpensesBaseAmount() {
        if (this.expenses == null || this.expenses.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return this.expenses.stream()
                .map(expense -> expense.getBaseAmount() == null ? BigDecimal.ZERO : expense.getBaseAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getExpensesVatAmount() {
        if (this.expenses == null || this.expenses.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return this.expenses.stream()
                .map(expense -> {
                    BigDecimal expenseBase = expense.getBaseAmount() == null ? BigDecimal.ZERO : expense.getBaseAmount();
                    BigDecimal expenseVatRate = expense.getVatRate() == null
                            ? BigDecimal.ZERO
                            : new BigDecimal(expense.getVatRate());
                    return expenseBase.multiply(expenseVatRate)
                            .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalBaseAmount() {
        BigDecimal effectiveBaseAmount = this.baseAmount == null ? BigDecimal.ZERO : this.baseAmount;
        return effectiveBaseAmount.add(this.getExpensesBaseAmount());
    }

    public BigDecimal getTotalVatAmount() {
        return this.getServiceVatAmount().add(this.getExpensesVatAmount());
    }

    public BigDecimal getTotalAmount() {
        return this.getTotalBaseAmount().add(this.getTotalVatAmount());
    }

    public BigDecimal getPendingBaseAmount() {
        BigDecimal pendingServiceBase = this.baseAmount == null ? BigDecimal.ZERO : this.baseAmount;
        BigDecimal paidServiceTotal = this.getInvoicedPaymentsTotalAmount();
        if (paidServiceTotal.compareTo(BigDecimal.ZERO) > 0 && this.vatRate != null) {
            BigDecimal divisor = BigDecimal.ONE.add(this.vatRate
                    .divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP));
            BigDecimal paidServiceBase = paidServiceTotal.divide(divisor, 4, RoundingMode.HALF_UP);
            pendingServiceBase = pendingServiceBase.subtract(paidServiceBase);
            if (pendingServiceBase.compareTo(BigDecimal.ZERO) < 0) {
                pendingServiceBase = BigDecimal.ZERO;
            }
        }
        return pendingServiceBase.add(this.getExpensesBaseAmount());
    }

    public BigDecimal getPendingVatAmount() {
        BigDecimal pendingServiceVat = this.getServiceVatAmount();
        BigDecimal paidServiceTotal = this.getInvoicedPaymentsTotalAmount();
        if (paidServiceTotal.compareTo(BigDecimal.ZERO) > 0 && this.vatRate != null) {
            BigDecimal divisor = BigDecimal.ONE.add(this.vatRate
                    .divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP));
            BigDecimal paidServiceBase = paidServiceTotal.divide(divisor, 4, RoundingMode.HALF_UP);
            BigDecimal paidServiceVat = paidServiceTotal.subtract(paidServiceBase);
            pendingServiceVat = pendingServiceVat.subtract(paidServiceVat);
            if (pendingServiceVat.compareTo(BigDecimal.ZERO) < 0) {
                pendingServiceVat = BigDecimal.ZERO;
            }
        }
        return pendingServiceVat.add(this.getExpensesVatAmount());
    }

    private BigDecimal getInvoicedPaymentsTotalAmount() {
        if (this.invoicedPayments == null || this.invoicedPayments.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return this.invoicedPayments.stream()
                .map(payment -> payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
