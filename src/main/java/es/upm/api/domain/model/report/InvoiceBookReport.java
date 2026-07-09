package es.upm.api.domain.model.report;

import es.upm.api.domain.model.BillingInfo;
import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.SupplierInfo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public record InvoiceBookReport(
        String reference,
        Quarter quarter,
        LocalDate operationDate,
        LocalDate emissionDate,
        String clientName,
        String clientNif,
        Map<Integer, VatLine> vatLines,
        String category
) {
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int SCALE = 6;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static InvoiceBookReport from(Invoice invoice) {
        BillingInfo bi = invoice.getBillingInfo();
        LocalDate date = invoice.getOperationDate();
        TreeMap<Integer, VatLine> vatLines = new TreeMap<>(Comparator.reverseOrder());
        addVatLine(vatLines, invoice.getVatRate().intValue(), invoice.getBaseAmount(), invoice.getVatAmount());

        if (Boolean.TRUE.equals(invoice.getClosed())) {
            BigDecimal percentageFactor = invoice.getPercentage() == null
                    ? BigDecimal.ONE
                    : invoice.getPercentage().divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
            Optional.ofNullable(invoice.getExpenses())
                    .orElse(List.of())
                    .forEach(expense -> {
                        BigDecimal baseAmount = expense.getBaseAmount().multiply(percentageFactor);
                        BigDecimal vatAmount = expense.vatAmount().multiply(percentageFactor);
                        addVatLine(vatLines, expense.getVatRate(), baseAmount, vatAmount);
                    });
        }

        return new InvoiceBookReport(
                invoice.getSeries() + "-" + invoice.getNumber(),
                Quarter.from(date),
                invoice.getOperationDate(),
                invoice.getEmissionDate(),
                bi.getFullName(),
                bi.getIdentity(),
                vatLines,
                ""
        );
    }

    private static void addVatLine(Map<Integer, VatLine> vatLines, Integer vatRate,
                                   BigDecimal baseAmount, BigDecimal vatAmount) {
        VatLine current = vatLines.getOrDefault(vatRate,
                new VatLine(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        BigDecimal totalAmount = baseAmount.add(vatAmount);
        vatLines.put(vatRate, new VatLine(
                current.baseAmount().add(baseAmount),
                current.vatAmount().add(vatAmount),
                current.totalAmount().add(totalAmount)
        ));
    }

    public static InvoiceBookReport from(Expense expense, Quarter quarter) {
        SupplierInfo supplier = expense.getSupplier();
        BigDecimal baseAmount = expense.deductibleBaseAmount();
        BigDecimal vatAmount = expense.deductibleVatAmount();
        TreeMap<Integer, VatLine> vatLines = new TreeMap<>(Comparator.reverseOrder());
        vatLines.put(expense.getVatRate(), new VatLine(baseAmount, vatAmount, baseAmount.add(vatAmount)));

        return new InvoiceBookReport(
                expense.getSeries() + "-" + expense.getNumber(),
                quarter,
                expense.getRecordedAt().toLocalDate(),
                expense.getIssueDate(),
                supplier.getName(),
                supplier.getIdentity(),
                vatLines,
                expense.getTaxCategory().name()
        );
    }

    private static NumberFormat numberFormat() {
        NumberFormat amount = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-ES"));
        amount.setGroupingUsed(false);
        amount.setMinimumFractionDigits(2);
        amount.setMaximumFractionDigits(2);
        return amount;
    }

    // Libro de emitidas: columnas por tipo de IVA, necesita allRates para alinear
    public String toInvoiceCsvLine(SortedSet<Integer> allRates) {
        NumberFormat amount = numberFormat();
        List<String> fields = new ArrayList<>(this.commonFields());
        VatLine empty = new VatLine(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        allRates.forEach(rate -> {
            VatLine vatLine = this.vatLines.getOrDefault(rate, empty);
            fields.add(amount.format(vatLine.baseAmount()));
            fields.add(rate + "%");
            fields.add(amount.format(vatLine.vatAmount()));
            fields.add(amount.format(vatLine.totalAmount()));
        });
        return String.join(";", fields);
    }

    // Libro de recibidas: un solo tipo por fila, el tipo es una columna más
    public String toExpenseCsvLine() {
        NumberFormat amount = numberFormat();
        Map.Entry<Integer, VatLine> entry = this.vatLines.entrySet().iterator().next();
        VatLine vatLine = entry.getValue();
        List<String> fields = new ArrayList<>(this.commonFields());
        fields.add(amount.format(vatLine.baseAmount()));
        fields.add(entry.getKey() + "%");
        fields.add(amount.format(vatLine.vatAmount()));
        fields.add(amount.format(vatLine.totalAmount()));
        fields.add(category);
        return String.join(";", fields);
    }

    private List<String> commonFields() {
        return List.of(
                this.reference,
                this.quarter.name(),
                DATE.format(this.operationDate),
                DATE.format(this.emissionDate),
                this.clientName,
                this.clientNif
        );
    }

}
