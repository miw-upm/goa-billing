package es.upm.api.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import es.upm.api.domain.model.creation.LegalProcedure;
import es.upm.api.domain.model.external.EngagementSnapshot;
import jakarta.validation.Valid;
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
    private static final int SCALE = 6;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private UUID id;
    private String concept;
    private Boolean closed;
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
    private List<LegalProcedure> legalProcedures;
    private List<Payment> payments;
    private List<Payment> priorPayments;
    private List<Expense> expenses;
    private List<BigDecimal> discounts;

    @Valid
    private OriginalInvoice originalInvoice;

    private String pdfPath;


    public boolean isIssued() {
        return this.emissionDate != null;
    }

    public boolean isRectification() {
        return this.originalInvoice != null;
    }

    // === Factores ===
    public BigDecimal vatFactor() {        // 0.21 Base*vatFactor = IVA
        return vatRate.divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal vatTotal() {         // 1.21 Base * vatTotal = Total
        return BigDecimal.ONE.add(vatFactor());
    }

    public BigDecimal baseShare() {        // 1/1.21  → bruto × baseShare = base
        return BigDecimal.ONE.divide(vatTotal(), SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal percentageFactor() { // 0.60
        return percentage.divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
    }

    // === Sumatorios ===
    private <T> BigDecimal sum(List<T> list, Function<T, BigDecimal> mapper) {
        return Optional.ofNullable(list)
                .orElse(List.of())
                .stream()
                .map(mapper)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // === Pagos de la presente ===
    public BigDecimal paymentsAmount() {
        return sum(payments, Payment::getAmount);
    }

    public BigDecimal paymentsBaseAmount() {
        return sum(payments, p -> p.getAmount().multiply(baseShare())).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal paymentsVatAmount() {
        return paymentsAmount().subtract(paymentsBaseAmount());
    }

    // === Pagos anteriores ===
    public BigDecimal priorPaymentsAmount() {
        return sum(priorPayments, Payment::getAmount);
    }

    public BigDecimal priorPaymentsBaseAmount() {
        return sum(priorPayments, p -> p.getAmount().multiply(baseShare())).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal priorPaymentsVatAmount() {
        return priorPaymentsAmount().subtract(priorPaymentsBaseAmount());
    }

    // === Gastos ===
    public BigDecimal expensesBaseAmount() {
        return sum(expenses, Expense::getBaseAmount);
    }

    public BigDecimal expensesVatAmount() {
        return sum(expenses, expense -> {
            if (expense.getBaseAmount() == null || expense.getVatRate() == null) {
                return BigDecimal.ZERO;
            }
            return expense.getBaseAmount()
                    .multiply(BigDecimal.valueOf(expense.getVatRate()))
                    .divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
        });
    }

    // === Descuentos ===
    public BigDecimal discountsAmount() {
        return sum(discounts, Function.identity());
    }

    public BigDecimal totalBudget() {
        if (legalProcedures != null) {
            return sum(legalProcedures, LegalProcedure::getBudget);
        }
        return baseAmount;
    }

    // === Mutadores ===
    public void applyVatRate(BigDecimal vatRate) {
        this.vatRate = vatRate;
        this.vatAmount = baseAmount.multiply(vatFactor());
    }

    public void applyBaseAmount(BigDecimal baseAmount) {
        this.baseAmount = baseAmount;
        this.vatAmount = baseAmount.multiply(vatFactor());
    }

    public void applyTotalAmount(BigDecimal totalAmount) {
        this.baseAmount = totalAmount.multiply(this.baseShare()).setScale(SCALE, RoundingMode.HALF_UP);
        this.vatAmount = totalAmount.subtract(this.baseAmount).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal baseFromTotal(BigDecimal total) {
        return total.divide(
                BigDecimal.ONE.add(vatRate
                        .divide(HUNDRED, SCALE, RoundingMode.HALF_UP)), SCALE, RoundingMode.HALF_UP
        );
    }

    public BigDecimal applyPercentage(BigDecimal value) {
        return value.multiply(percentage).divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
    }
}
