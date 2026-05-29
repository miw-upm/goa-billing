package es.upm.api.domain.services;

import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.external.UserSnapshot;
import es.upm.api.domain.ports.out.user.UserFinder;
import es.upm.miw.pdf.PdfBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class InvoicePdfService {
    public static final BigDecimal MIN_VALUE_FOR_TRANSFER = new BigDecimal("5");
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", new Locale("es", "ES"));
    private static final NumberFormat EUR = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-ES"));

    private final UserFinder userFinder;

    public byte[] generatePdf(Invoice invoice, boolean original) {
        String title = invoice.isIssued() ? "FACTURA  " : "FACTURA PROFORMA";
        title = original ? title + "   ORIGINAL" : title + "  COPIA (" + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")";
        String invoiceNumber = invoice.isIssued()
                ? invoice.getSeries() + "-" + invoice.getNumber()
                : "—";
        String issueDate = invoice.isIssued()
                ? invoice.getEmissionDate().format(DATE_FORMAT)
                : "—";

        PdfBuilder pdf = new PdfBuilder()
                .header()
                .title(title)
                .space()
                .paragraphBold("Nº " + invoiceNumber + "   ·   " + issueDate);

        pdf.section("FACTURAR A, CON PARTICIPACIÓN DEL : " + invoice.getPercentage() + " %")
                .paragraphBold(invoice.getBillingInfo().getFullName())
                .paragraph("N.I.F. " + invoice.getBillingInfo().getIdentity())
                .paragraph(invoice.getBillingInfo().getFullAddress());

        pdf.section("CONCEPTO");
        if (invoice.getClosed() != null) {
            if (invoice.getClosed()) {
                pdf.paragraphBold("Factura por cierre de Hoja de Encargos.");
            } else {
                pdf.paragraphBold("Factura por ingreso de Provisión de Fondos.");
            }
        }
        if (Objects.nonNull(invoice.getLegalProcedures())) {
            invoice.getLegalProcedures()
                    .forEach(procedure -> pdf.paragraph(procedure.getTitle() + "  -  " +
                                    EUR.format(procedure.getBudget()))
                            .list(procedure.getLegalTasks()));
        }
        if (Objects.nonNull(invoice.getConcept())) {
            pdf.paragraph(invoice.getConcept());
        }

        pdf.section("IMPORTE DE LA PRESENTE FACTURA");
        BigDecimal totalAmount = invoice.getBaseAmount().add(invoice.getVatAmount());
        BigDecimal baseExpense = invoice.applyPercentage(invoice.expensesBaseAmount());
        BigDecimal vatExpense = invoice.applyPercentage(invoice.expensesVatAmount());
        BigDecimal totalExpense = baseExpense.add(vatExpense);
        pdf.table(
                new String[]{"Concepto", "Base Imponible", "IVA", "Total"},
                List.of(
                        new String[]{"Honorarios", EUR.format(invoice.getBaseAmount()), EUR.format(invoice.getVatAmount())
                                + "   (" + invoice.getVatRate().toPlainString() + "%)",
                                EUR.format(totalAmount)
                        },
                        new String[]{"Gastos", EUR.format(baseExpense),
                                EUR.format(vatExpense),
                                EUR.format(totalExpense)
                        }
                ),
                new String[]{"TOTAL",
                        EUR.format(invoice.getBaseAmount().add(baseExpense)),
                        EUR.format(invoice.getVatAmount().add(vatExpense)),
                        EUR.format(totalAmount.add(totalExpense))
                }
        );
        BigDecimal debt = BigDecimal.ZERO;
        if (invoice.getClosed() != null && invoice.getClosed()) {
            debt = totalAmount.add(totalExpense).subtract((invoice.paymentsAmount()).multiply(invoice.percentageFactor()));
        }
        if (debt.compareTo(MIN_VALUE_FOR_TRANSFER) > 0) {
            pdf.paragraphHighlight("PENDIENTE DE INGRESAR: " + EUR.format(debt));
            pdf.paragraphHighlight("Ruego que ingrese en la cuenta bancaria: ES00 1111 2222 3333 4444 5555");
        }

        pdf.signatureLine("Doña Nuria Ocaña Pérez");

        pdf.pageBreak().section("Información detallada");

        if (invoice.getDiscounts() != null && !invoice.getDiscounts().isEmpty()) {
            pdf.paragraphBold("Descuentos aplicados");
            List<String[]> discountRows = invoice.getDiscounts().stream()
                    .map(discount -> new String[]{
                            "",
                            "− " + EUR.format(discount),
                            ""
                    }).toList();
            pdf.table(
                    new String[]{"Base original", "Descuentos", "Base final"},
                    discountRows,
                    new String[]{
                            EUR.format(invoice.totalBudget()),
                            EUR.format(invoice.discountsAmount()),
                            EUR.format(invoice.totalBudget().subtract(invoice.discountsAmount()))},
                    new String[]{"TOTAL " + invoice.getPercentage() + "%", "",
                            this.applyPercentage(invoice.getPercentage(), invoice.totalBudget().subtract(invoice.discountsAmount()))
                    }
            );
        }

        if (invoice.getPayments() != null && !invoice.getPayments().isEmpty()) {
            pdf.paragraphBold("Ingresos Asociados a la Hoja de Encargo y facturados en la presente");
            List<String[]> paymentRows = invoice.getPayments().stream()
                    .map(payment -> new String[]{
                            this.userFinder.readById(payment.getUser().getId()).toFullName(),
                            payment.getDate().format(DATE_FORMAT),
                            EUR.format(invoice.baseFromTotal(payment.getAmount())),
                            EUR.format(payment.getAmount().subtract(invoice.baseFromTotal(payment.getAmount()))),
                            EUR.format(payment.getAmount()),
                            payment.getMethod().name()
                    }).toList();
            BigDecimal base = invoice.paymentsBaseAmount();
            BigDecimal vat = invoice.paymentsAmount().subtract(base);
            pdf.table(
                    new String[]{"Cliente", "Fecha", "Base Imponible", "IVA", "Importe", "Tipo de Ingreso"},
                    paymentRows,
                    new String[]{"TOTAL", "", EUR.format(base), EUR.format(vat), EUR.format(invoice.paymentsAmount()), ""},
                    new String[]{"TOTAL " + invoice.getPercentage() + "%", "", this.applyPercentage(invoice.getPercentage(), base),
                            this.applyPercentage(invoice.getPercentage(), vat),
                            this.applyPercentage(invoice.getPercentage(), invoice.paymentsAmount()), ""
                    }
            );
        }

        if (invoice.getExpenses() != null && !invoice.getExpenses().isEmpty()) {
            pdf.paragraphBold("Gastos asociados a la Hoja de Encargo");
            List<String[]> expenseRows = invoice.getExpenses().stream()
                    .map(expense -> {
                        BigDecimal base = expense.getBaseAmount().setScale(6, RoundingMode.HALF_UP);
                        BigDecimal vat = base
                                .multiply(BigDecimal.valueOf(expense.getVatRate()))
                                .divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
                        return new String[]{
                                expense.getIssueDate().format(DATE_FORMAT),
                                expense.getDescription(),
                                EUR.format(base),
                                EUR.format(vat),
                                EUR.format(base.add(vat))
                        };
                    }).toList();

            pdf.table(
                    new String[]{"Fecha", "Descripción", "Base imponible", "IVA", "TOTAL"},
                    expenseRows,
                    new String[]{"TOTAL", "", EUR.format(invoice.expensesBaseAmount()),
                            EUR.format(invoice.expensesVatAmount()),
                            EUR.format(invoice.expensesBaseAmount().add(invoice.expensesVatAmount()))},
                    new String[]{"TOTAL " + invoice.getPercentage() + "%", "",
                            this.applyPercentage(invoice.getPercentage(), invoice.expensesBaseAmount()),
                            this.applyPercentage(invoice.getPercentage(), invoice.expensesVatAmount()),
                            this.applyPercentage(invoice.getPercentage(), invoice.expensesBaseAmount().add(invoice.expensesVatAmount()))
                    }
            );
        }

        if (invoice.getPriorPayments() != null && !invoice.getPriorPayments().isEmpty()) {
            pdf.paragraphBold("Anteriores ingresos, Ya Facturados, de la Hoja de Encargo");
            List<String[]> paymentRows = invoice.getPriorPayments().stream()
                    .map(payment -> new String[]{
                            this.userFinder.readById(payment.getUser().getId()).toFullName(),
                            payment.getDate().format(DATE_FORMAT),
                            EUR.format(invoice.baseFromTotal(payment.getAmount())),
                            EUR.format(payment.getAmount().subtract(invoice.baseFromTotal(payment.getAmount()))),
                            EUR.format(payment.getAmount()),
                            payment.getMethod().name()
                    }).toList();
            BigDecimal base = invoice.baseFromTotal(invoice.priorPaymentsAmount());
            BigDecimal vat = invoice.priorPaymentsAmount().subtract(base);
            pdf.table(
                    new String[]{"Cliente", "Fecha", "Base Imponible", "IVA", "Importe", "Tipo de Ingreso"},
                    paymentRows,
                    new String[]{"TOTAL", "", EUR.format(base), EUR.format(vat), EUR.format(invoice.priorPaymentsAmount()), ""},
                    new String[]{"TOTAL " + invoice.getPercentage() + "%", "", this.applyPercentage(invoice.getPercentage(), base),
                            this.applyPercentage(invoice.getPercentage(), vat),
                            this.applyPercentage(invoice.getPercentage(), invoice.priorPaymentsAmount()), ""}
            );
        }

        return pdf.build();
    }

    private String applyPercentage(BigDecimal percentage, BigDecimal value) {
        if (percentage.compareTo(new BigDecimal("100")) < 0) {
            return EUR.format(value.multiply(percentage).divide(new BigDecimal("100"), RoundingMode.HALF_UP));
        } else {
            return "";
        }
    }
}
