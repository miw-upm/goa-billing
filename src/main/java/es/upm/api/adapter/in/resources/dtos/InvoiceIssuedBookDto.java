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
        LocalDate emissionDate,
        LocalDate operationDate,
        String clientNif,
        String clientName,
        BigDecimal baseAmount,
        int vatRate,
        BigDecimal vatAmount,
        BigDecimal totalAmount
) {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String toCsvLine() {
        NumberFormat eur = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-ES"));
        return String.join(";",
                this.invoiceNumber,
                this.quarter,
                DATE.format(this.emissionDate),
                DATE.format(this.operationDate),
                this.clientNif,
                this.clientName,
                eur.format(this.baseAmount),
                String.valueOf(this.vatRate),
                eur.format(this.vatAmount),
                eur.format(this.totalAmount)
        );
    }

    public static InvoiceIssuedBookDto from(Invoice invoice) {
        BillingInfo bi = invoice.getBillingInfo();
        LocalDate date = invoice.getOperationDate();
        BigDecimal baseAmount = invoice.getBaseAmount();
        BigDecimal vatAmount = invoice.getVatAmount();
        return new InvoiceIssuedBookDto(
                invoice.getSeries() + "-" + String.format("%05d", invoice.getNumber()),
                Quarter.from(date).name(),
                invoice.getEmissionDate(),
                invoice.getOperationDate(),
                bi.getIdentity(),
                bi.getFullName(),
                baseAmount,
                invoice.getVatRate().intValue(),
                vatAmount,
                baseAmount.add(vatAmount)
        );
    }

}
