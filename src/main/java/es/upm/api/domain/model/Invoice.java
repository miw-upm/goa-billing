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
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.*;
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
    private BigDecimal baseExpense;
    private BigDecimal vatExpense;
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

    public void applyDefaults() {
        if (Objects.isNull(this.percentage)) {
            this.percentage = HUNDRED;
        }
        if (Objects.isNull(this.baseExpense)) {
            this.baseExpense = BigDecimal.ZERO;
        }
        if (Objects.isNull(this.vatExpense)) {
            this.vatExpense = BigDecimal.ZERO;
        }
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

    public String buildConceptString() {
        StringBuilder sb = new StringBuilder();
        if (Boolean.TRUE.equals(closed)) {
            sb.append("Factura por cierre de Hoja de Encargos.\n");
        } else if (Boolean.FALSE.equals(closed)) {
            sb.append("Factura por ingreso de Provisión de Fondos.\n");
        }
        if (legalProcedures != null) {
            legalProcedures.forEach(p -> {
                sb.append(p.getTitle()).append("  -  ")
                        .append(NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-ES")).format(p.getBudget()))
                        .append("\n");
                p.getLegalTasks().forEach(task -> sb.append("  • ").append(task).append("\n"));
            });
        }
        if (concept != null) {
            sb.append(concept).append("\n");
        }
        return sb.toString();
    }

}
