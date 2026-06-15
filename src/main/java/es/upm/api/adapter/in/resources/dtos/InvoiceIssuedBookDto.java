package es.upm.api.adapter.in.resources.dtos;

import es.upm.api.adapter.in.resources.Quarter;
import es.upm.api.domain.model.BillingInfo;
import es.upm.api.domain.model.Invoice;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public record InvoiceIssuedBookDto(
        String invoiceNumber,
        String quarter,
        LocalDate operationDate,
        LocalDate emissionDate,
        String clientName,
        String clientNif,
        BigDecimal baseAmount,
        BigDecimal vatRate,
        BigDecimal vatAmount,
        BigDecimal totalAmount
) {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String toCsvLine() {
        NumberFormat amount = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-ES"));
        amount.setGroupingUsed(false);
        amount.setMinimumFractionDigits(2);
        amount.setMaximumFractionDigits(2);
        return String.join(";",
                this.invoiceNumber,
                this.quarter,
                DATE.format(this.operationDate),
                DATE.format(this.emissionDate),
                this.clientName,
                this.clientNif,
                amount.format(this.baseAmount),
                amount.format(this.vatRate),
                amount.format(this.vatAmount),
                amount.format(this.totalAmount)
        );
    }

    public static InvoiceIssuedBookDto from(Invoice invoice) {
        BillingInfo bi = invoice.getBillingInfo();
        LocalDate date = invoice.getOperationDate();
        BigDecimal baseAmount = invoice.getBaseAmount();
        BigDecimal vatAmount = invoice.getVatAmount();
        return new InvoiceIssuedBookDto(
                invoice.getSeries() + "-" + invoice.getNumber(),
                Quarter.from(date).name(),
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

}
