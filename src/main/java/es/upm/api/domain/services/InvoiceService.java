package es.upm.api.domain.services;

import es.upm.api.domain.model.*;
import es.upm.api.domain.model.creation.InvoiceCreationFromEngagement;
import es.upm.api.domain.model.criteria.InvoiceFindCriteria;
import es.upm.api.domain.model.external.EngagementSnapshot;
import es.upm.api.domain.model.external.UserSnapshot;
import es.upm.api.domain.ports.out.billing.ExpenseGateway;
import es.upm.api.domain.ports.out.billing.InvoiceGateway;
import es.upm.api.domain.ports.out.billing.PaymentGateway;
import es.upm.api.domain.ports.out.engagement.EngagementFinder;
import es.upm.api.domain.ports.out.user.UserFinder;
import es.upm.miw.exception.InvalidTransitionException;
import es.upm.miw.pdf.PdfBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class InvoiceService {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", new Locale("es", "ES"));


    private static final BigDecimal DEFAULT_VAT_RATE = new BigDecimal("21");

    private final InvoiceGateway invoiceGateway;
    private final PaymentGateway paymentGateway;
    private final ExpenseGateway expenseGateway;
    private final EngagementFinder engagementFinder;
    private final UserFinder userFinder;

    public void create(Invoice invoice) {
        invoice.setId(UUID.randomUUID());
        invoice.getBillingInfo().updateFrom(this.userFinder.readById(invoice.getBillingInfo().getUserId()));
        invoice.applyVatRate(DEFAULT_VAT_RATE);
        this.invoiceGateway.create(invoice);
    }

    private List<InvoicedPayment> priorPayments(UUID engagementId) {
        return this.paymentGateway
                .findInvoicedByEngagementId(engagementId)
                .map(payment -> {
                    payment.setUser(this.userFinder.readById(payment.getUser().getId()));
                    return new InvoicedPayment(payment);
                })
                .toList();
    }

    private List<InvoicedPayment> payments(UUID engagementId) {
        return this.paymentGateway
                .findNotInvoicedByEngagementId(engagementId)
                .map(payment -> {
                    payment.setUser(this.userFinder.readById(payment.getUser().getId()));
                    return new InvoicedPayment(payment);
                })
                .toList();
    }

    private InvoicedExpense toInvoicedExpense(Expense expense) {
        BigDecimal vatAmount = expense.getBaseAmount()
                .multiply(BigDecimal.valueOf(expense.getVatRate()))
                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        return new InvoicedExpense(
                expense.getId(),
                expense.getIssueDate(),
                expense.getDescription(),
                expense.getBaseAmount(),
                vatAmount
        );
    }

    public void createFromEngagement(InvoiceCreationFromEngagement creation) {
        EngagementSnapshot engagement = this.engagementFinder.read(creation.getEngagementId());
        List<InvoicedExpense> expenses;
        if (Boolean.TRUE.equals(creation.getCloseEngagement())) {
            expenses = this.expenseGateway.findByEngagementId(creation.getEngagementId())
                    .map(this::toInvoicedExpense)
                    .toList();
        } else {
            expenses = null;
        }
        Invoice invoice = Invoice.builder()
                .billingInfo(BillingInfo.builder()
                        .concept("FACTURA por ingreso de Provisón de Fondos." + System.lineSeparator() + System.lineSeparator()
                                + creation.buildProceduresText())
                        .build()
                )
                .vatRate(DEFAULT_VAT_RATE)
                .engagement(engagement)
                .payments(this.payments(creation.getEngagementId()))
                .priorPayments(this.priorPayments(creation.getEngagementId()))
                .expenses(expenses)
                .build();
        if (Boolean.TRUE.equals(creation.getCloseEngagement())) {
            invoice.applyBaseAmount(creation.totalBudget());
        } else {
            invoice.applyBaseAmount(invoice.paymentsAmount().add(invoice.priorPaymentsAmount()));
        }
        creation.getBillingPercentages().stream()
                .filter(userPercentage -> userPercentage.getPercentage().compareTo(BigDecimal.ZERO) > 0)
                .forEach(userPercentage -> {
                    invoice.setId(UUID.randomUUID());
                    invoice.setPercentage(userPercentage.getPercentage());
                    invoice.getBillingInfo().updateFrom(this.userFinder.readById(userPercentage.getUserId()));
                    this.invoiceGateway.create(invoice);
                });
    }

    public void emission(UUID id) {
        Invoice invoice = this.read(id);
        if (invoice.getEmissionDate() != null) {
            throw new IllegalStateException("Already invoice issued: " + id);
        }
        String series = String.valueOf(LocalDate.now().getYear());
        invoice.setSeries(series);
        invoice.setNumber(invoiceGateway.findNextNumber(series));
        invoice.setEmissionDate(LocalDate.now());
        this.invoiceGateway.update(id, invoice);
        if (invoice.getPayments() != null) {
            invoice.getPayments().forEach(invoicedPayment -> {
                Payment payment = this.paymentGateway.read(invoicedPayment.paymentId());
                payment.setInvoiced(true);
                paymentGateway.update(invoicedPayment.paymentId(), payment);
            });
        }
        //TODO enviar por email
    }

    public Invoice read(UUID id) {
        Invoice invoice = this.invoiceGateway.read(id);
        if (invoice.getEngagement() != null) {
            invoice.setEngagement(this.engagementFinder.read(invoice.getEngagement().getId()));
        }
        return invoice;
    }

    public Invoice update(UUID id, Invoice invoice) { //TODO no tengo claro que se puede actualizar
        Invoice currentInvoice = this.invoiceGateway.read(id);
        if (invoice.getEmissionDate() != null) {
            throw new InvalidTransitionException("Issued invoices cannot be updated, id: " + id);
        }
        invoice.setId(id);
        invoice.setEmissionDate(null);
        invoice.setPdfPath(currentInvoice.getPdfPath());
        this.validateAndHydrate(invoice);
        return this.invoiceGateway.update(id, invoice);
    }

    public void delete(UUID id) {
        this.invoiceGateway.delete(id);
    }

    public Stream<Invoice> find(InvoiceFindCriteria criteria) {
        Stream<Invoice> invoices = invoiceGateway.find(criteria);

        if (StringUtils.hasText(criteria.getClient())) {
            List<UUID> clientIds = userFinder.find(criteria.getClient()).stream()
                    .map(UserSnapshot::getId)
                    .toList();

            invoices = invoices.filter(invoice ->
                    invoice.getBillingInfo() != null
                            && invoice.getBillingInfo().getUserId() != null
                            && clientIds.contains(invoice.getBillingInfo().getUserId()));
        }

        return invoices.map(invoice -> {
            if (invoice.getEngagement() != null) {
                UUID engagementId = invoice.getEngagement().getId();
                invoice.setEngagement(engagementFinder.read(engagementId));
            }
            return invoice;
        });
    }

    private void validateAndHydrate(Invoice invoice) {
        if (invoice.getEngagement() != null && invoice.getEngagement().getId() != null) {
            invoice.setEngagement(this.engagementFinder.read(invoice.getEngagement().getId()));
        }
        invoice.setBillingInfo(this.hydrateBillingInfo(invoice.getBillingInfo()));

        if (invoice.getPayments() != null && !invoice.getPayments().isEmpty()) {
            BigDecimal paymentsTotal = invoice.getPayments().stream()
                    .map(InvoicedPayment::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal discountsTotal = invoice.getDiscounts() == null ? BigDecimal.ZERO
                    : invoice.getDiscounts().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            invoice.setBaseAmount(paymentsTotal.subtract(discountsTotal));
        }
        if (invoice.getVatRate() == null) {
            invoice.setVatRate(DEFAULT_VAT_RATE);
        }
        if (invoice.getVatAmount() == null && invoice.getBaseAmount() != null) {
            invoice.setVatAmount(invoice.getBaseAmount()
                    .multiply(invoice.getVatRate())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
        }
    }

    private BillingInfo hydrateBillingInfo(BillingInfo billingInfo) {
        if (billingInfo == null || billingInfo.getUserId() == null) {
            return billingInfo;
        }
        billingInfo.updateFrom(this.userFinder.readById(billingInfo.getUserId()));
        billingInfo.setConcept(billingInfo.getConcept());
        return billingInfo;
    }

    public byte[] generatePdf(UUID id) {
        Invoice invoice = this.read(id);

        BigDecimal baseAmount = invoice.totalBaseAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal vatAmount = invoice.totalVatAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = baseAmount.add(vatAmount);
        BigDecimal vatRate = invoice.getVatRate();

        String title = invoice.isIssued() ? "FACTURA" : "FACTURA PROFORMA";
        String invoiceNumber = invoice.isIssued()
                ? invoice.getSeries() + "-" + invoice.getNumber()
                : "—";
        String issueDate = invoice.isIssued()
                ? invoice.getEmissionDate().format(DATE_FORMAT)
                : "—";

        PdfBuilder pdf = new PdfBuilder()
                .header()
                .space(2)
                .title(title)
                .paragraphBold("Nº " + invoiceNumber + "   ·   " + issueDate)
                .space(2);

        pdf.section("FACTURAR A")
                .paragraphBold(invoice.getBillingInfo().getFullName())
                .paragraph("N.I.F. " + invoice.getBillingInfo().getIdentity())
                .paragraph(invoice.getBillingInfo().getFullAddress())
                .space();

        pdf.section("CONCEPTO")
                .paragraph(invoice.getBillingInfo().getConcept())
                .space();

        pdf.section("PARTICIPACIÓN DEL CLIENTE")
                .paragraph(invoice.getPercentage() + " %")
                .space();

        pdf.section("IMPORTE DE LA PRESENTE FACTURA");
        List<String[]> amountRows = List.of(
                new String[]{"Base imponible", baseAmount.toPlainString() + " €"},
                new String[]{"IVA (" + vatRate.toPlainString() + "%)", vatAmount.toPlainString() + " €"},
                new String[]{"TOTAL", totalAmount.toPlainString() + " €"}
        );
        pdf.table(new String[]{"Concepto", "Importe"}, amountRows);

        if (invoice.getDiscounts() != null && !invoice.getDiscounts().isEmpty()) {
            pdf.section("DESCUENTOS APLICADOS A LA HOJA DE ENCARGO");
            List<String[]> discountRows = new ArrayList<>(invoice.getDiscounts().stream()
                    .map(d -> new String[]{
                            "",
                            "− " + d.setScale(2, RoundingMode.HALF_UP).toPlainString() + " €",
                            ""
                    })
                    .toList());
            discountRows.add(new String[]{
                    invoice.totalBaseAmount().add(invoice.discountsAmount()).setScale(2, RoundingMode.HALF_UP).toPlainString() + " €",
                    "− " + invoice.discountsAmount().setScale(2, RoundingMode.HALF_UP).toPlainString() + " €",
                    invoice.totalBaseAmount().setScale(2, RoundingMode.HALF_UP).toPlainString() + " €"
            });
            pdf.table(new String[]{"Base original", "Descuentos", "Base final"}, discountRows);
        }

        if (invoice.getPayments() != null && !invoice.getPayments().isEmpty()) {
            pdf.section("INGRESOS ASOCIADOS A LA PRESENTE FACTURA");
            List<String[]> paymentRows = invoice.getPayments().stream()
                    .map(p -> new String[]{
                            p.user().toFullName(),
                            p.date().format(DATE_FORMAT),
                            p.amount().setScale(2, RoundingMode.HALF_UP).toPlainString() + " €",
                            p.method().name()
                    })
                    .toList();
            pdf.table(new String[]{"Cliente", "Fecha", "Importe", "Tipo de Ingreso"}, paymentRows);
        }

        if (invoice.getExpenses() != null && !invoice.getExpenses().isEmpty()) {
            pdf.section("GASTOS ASOCIADOS A LA HOJA DE ENCARGO");

            List<String[]> expenseRows = invoice.getExpenses().stream()
                    .map(e -> {
                        BigDecimal base = e.baseAmount().setScale(2, RoundingMode.HALF_UP);
                        BigDecimal vat = e.vatAmount().setScale(2, RoundingMode.HALF_UP);
                        return new String[]{
                                e.issueDate().format(DATE_FORMAT),
                                e.description(),
                                base.toPlainString() + " €",
                                vat.toPlainString() + " €"
                        };
                    })
                    .toList();

            pdf.table(new String[]{"Fecha", "Descripción", "Base imponible", "IVA"}, expenseRows);
        }

        if (invoice.getPriorPayments() != null && !invoice.getPriorPayments().isEmpty()) {
            pdf.section("ANTERIORES INGRESOS YA FACTURADOS DE LA HOJA DE ENCARGO");
            List<String[]> paymentRows = invoice.getPriorPayments().stream()
                    .map(p -> new String[]{
                            p.user().toFullName(),
                            p.date().format(DATE_FORMAT),
                            p.amount().setScale(2, RoundingMode.HALF_UP).toPlainString() + " €",
                            p.method().name()
                    })
                    .toList();
            pdf.table(new String[]{"Cliente", "Fecha", "Importe", "Tipo de Ingreso"}, paymentRows);
        }


        pdf.space(3)
                .signatureLine("Doña Nuria Ocaña Pérez");

        return pdf.build();
    }


}
