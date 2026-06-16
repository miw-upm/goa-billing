package es.upm.api.adapter.in.resources.dtos;

import es.upm.api.adapter.in.resources.Quarter;
import es.upm.api.domain.model.BillingInfo;
import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.SupplierInfo;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record InvoiceBookDto(
        String reference,
        Quarter quarter,
        LocalDate operationDate,
        LocalDate emissionDate,
        String clientName,
        String clientNif,
        BigDecimal baseAmount,
        BigDecimal deductibleAmount,
        BigDecimal vatRate,
        BigDecimal vatAmount,
        BigDecimal totalAmount
) {
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String toCsvLine() {
        NumberFormat amount = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-ES"));
        amount.setGroupingUsed(false);
        amount.setMinimumFractionDigits(2);
        amount.setMaximumFractionDigits(2);
        List<String> values = new ArrayList<>(List.of(
                this.reference,
                this.quarter.name(),
                DATE.format(this.operationDate),
                DATE.format(this.emissionDate),
                this.clientName,
                this.clientNif,
                amount.format(this.baseAmount)
        ));
        if (this.deductibleAmount != null) {
            values.add(amount.format(this.deductibleAmount));
        }
        values.addAll(List.of(
                amount.format(this.vatRate),
                amount.format(this.vatAmount),
                amount.format(this.totalAmount)
        ));
        return String.join(";", values);
    }

    public static InvoiceBookDto from(Invoice invoice) {
        BillingInfo bi = invoice.getBillingInfo();
        LocalDate date = invoice.getOperationDate();
        BigDecimal baseAmount = invoice.getBaseAmount();
        BigDecimal vatAmount = invoice.getVatAmount();
        return new InvoiceBookDto(
                invoice.getSeries() + "-" + invoice.getNumber(),
                Quarter.from(date),
                invoice.getOperationDate(),
                invoice.getEmissionDate(),
                bi.getFullName(),
                bi.getIdentity(),
                baseAmount,
                null,
                invoice.vatFactor(),
                vatAmount,
                baseAmount.add(vatAmount)
        );
    }

    public static InvoiceBookDto from(Expense expense, int reference) {
        SupplierInfo supplier = expense.getSupplier();
        LocalDate date = expense.getIssueDate();
        BigDecimal baseAmount = expense.getBaseAmount();
        BigDecimal vatRate = BigDecimal.valueOf(expense.getVatRate()).divide(HUNDRED);
        BigDecimal vatAmount = baseAmount.multiply(vatRate);
        return new InvoiceBookDto(
                String.valueOf(reference),
                Quarter.from(date),
                date,
                date,
                supplier.getName(),
                supplier.getIdentity(),
                baseAmount,
                BigDecimal.ONE,
                vatRate,
                vatAmount,
                baseAmount.add(vatAmount)
        );
    }

}
