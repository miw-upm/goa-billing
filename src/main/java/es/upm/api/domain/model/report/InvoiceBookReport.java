package es.upm.api.domain.model.report;

import es.upm.api.domain.model.BillingInfo;
import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.SupplierInfo;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public record InvoiceBookReport(
        String reference,
        Quarter quarter,
        LocalDate operationDate,
        LocalDate emissionDate,
        String clientName,
        String clientNif,
        BigDecimal baseAmount,
        BigDecimal vatRate,
        BigDecimal vatAmount,
        BigDecimal totalAmount
) {
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static InvoiceBookReport from(Invoice invoice) {
        BillingInfo bi = invoice.getBillingInfo();
        LocalDate date = invoice.getOperationDate();
        BigDecimal baseAmount = invoice.getBaseAmount();
        BigDecimal vatAmount = invoice.getVatAmount();
        return new InvoiceBookReport(
                invoice.getSeries() + "-" + invoice.getNumber(),
                Quarter.from(date),
                invoice.getOperationDate(),
                invoice.getEmissionDate(),
                bi.getFullName(),
                bi.getIdentity(),
                baseAmount,
                invoice.vatFactor(),
                vatAmount,
                baseAmount.add(vatAmount)
        );
    }

    public static InvoiceBookReport from(Expense expense, Quarter quarter) {
        SupplierInfo supplier = expense.getSupplier();
        BigDecimal baseAmount = expense.deductibleBaseAmount();
        BigDecimal vatRate = BigDecimal.valueOf(expense.getVatRate()).divide(HUNDRED);
        BigDecimal vatAmount = expense.deductibleVatAmount();
        return new InvoiceBookReport(
                expense.getSeries() + "-" + expense.getNumber(),
                quarter,
                expense.getRecordedAt().toLocalDate(),
                expense.getIssueDate(),
                supplier.getName(),
                supplier.getIdentity(),
                baseAmount,
                vatRate,
                vatAmount,
                baseAmount.add(vatAmount)
        );
    }

    public String toCsvLine() {
        NumberFormat amount = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-ES"));
        amount.setGroupingUsed(false);
        amount.setMinimumFractionDigits(2);
        amount.setMaximumFractionDigits(2);
        return String.join(";", List.of(
                this.reference,
                this.quarter.name(),
                DATE.format(this.operationDate),
                DATE.format(this.emissionDate),
                this.clientName,
                this.clientNif,
                amount.format(this.baseAmount),
                amount.format(this.vatRate),
                amount.format(this.vatAmount),
                amount.format(this.totalAmount)
        ));
    }

}
