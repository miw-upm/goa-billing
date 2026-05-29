package es.upm.api.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import es.upm.api.domain.model.creation.LegalProcedure;
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

    private String pdfPath;
    private Rectification rectification;

    public boolean isIssued() {
        return this.emissionDate != null;
    }

    // === Factores ===
    public BigDecimal vatFactor() {        // 0.21 Base*vatFactor = IVA
        return vatRate.divide(HUNDRED, 6, RoundingMode.HALF_UP);
    }

    public BigDecimal vatTotal() {         // 1.21 Base * vatTotal = Total
        return BigDecimal.ONE.add(vatFactor());
    }

    public BigDecimal baseShare() {        // 1/1.21  → bruto × baseShare = base
        return BigDecimal.ONE.divide(vatTotal(), 6, RoundingMode.HALF_UP);
    }

    public BigDecimal percentageFactor() { // 0.60
        return percentage.divide(HUNDRED, 4, RoundingMode.HALF_UP);
    }

    /**
     * Base imponible contenida en un importe bruto (IVA incluido).
     */
    private BigDecimal baseOf(BigDecimal grossAmount) {
        return grossAmount.multiply(baseShare());
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
        return sum(payments, p -> baseOf(p.getAmount()));
    }

    public BigDecimal paymentsVatAmount() {
        return paymentsAmount().subtract(paymentsBaseAmount());
    }

    // === Pagos anteriores ===
    public BigDecimal priorPaymentsAmount() {
        return sum(priorPayments, Payment::getAmount);
    }

    public BigDecimal priorPaymentsBaseAmount() {
        return sum(priorPayments, p -> baseOf(p.getAmount()));
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
                    .divide(HUNDRED, 4, RoundingMode.HALF_UP);
        });
    }

    // === Descuentos ===
    public BigDecimal discountsAmount() {
        return sum(discounts, Function.identity());
    }

    // === Totales de la factura (aplicando %) ===
    public BigDecimal totalBaseAmount() {
        if (Boolean.TRUE.equals(closed)) {
            return baseAmount
                    .subtract(discountsAmount())
                    .subtract(priorPaymentsBaseAmount())
                    .multiply(percentageFactor());
        }
        return baseAmount.multiply(percentageFactor());
    }

    public BigDecimal totalVatAmount() {
        return totalBaseAmount().multiply(vatFactor());
    }

    public BigDecimal totalAmount() {
        return baseAmount
                .add(vatAmount)
                .add(expensesBaseAmount())
                .add(expensesVatAmount());
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
        this.baseAmount = baseOf(totalAmount);
        this.vatAmount = totalAmount.subtract(this.baseAmount);
    }

    public BigDecimal baseFromTotal(BigDecimal total) {
        return total.divide(
                BigDecimal.ONE.add(vatRate.divide(HUNDRED, 6, RoundingMode.HALF_UP)),
                2,
                RoundingMode.HALF_UP
        );
    }

    public BigDecimal applyPercentage(BigDecimal value) {
        return value.multiply(percentage).divide(HUNDRED, RoundingMode.HALF_UP);
    }
}
