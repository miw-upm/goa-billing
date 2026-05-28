package es.upm.api.domain.services;

import es.upm.api.domain.model.*;
import es.upm.api.domain.model.creation.InvoiceCreationFromEngagement;
import es.upm.api.domain.model.criteria.InvoiceFindCriteria;
import es.upm.api.domain.model.external.EngagementSnapshot;
import es.upm.api.domain.model.external.UserSnapshot;
import es.upm.api.domain.ports.out.billing.ExpenseGateway;
import es.upm.api.domain.ports.out.billing.InvoiceGateway;
import es.upm.api.domain.ports.out.billing.PaymentGateway;
import es.upm.api.domain.ports.out.email.EmailWriter;
import es.upm.api.domain.ports.out.engagement.EngagementGateway;
import es.upm.api.domain.ports.out.user.UserFinder;
import es.upm.miw.exception.BadRequestException;
import es.upm.miw.exception.InvalidTransitionException;
import es.upm.miw.pdf.PdfBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    private final EngagementGateway engagementGateway;
    private final UserFinder userFinder;
    private final EmailWriter emailWriter;
    private final InvoiceTemplateService invoiceTemplateService;
    @Value("${app.administration.name}")
    private String name;
    @Value("${app.administration.email}")
    private String email;

    public void create(Invoice invoice) {
        invoice.setId(UUID.randomUUID());
        invoice.setPercentage(new BigDecimal("100"));
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
        EngagementSnapshot engagement = this.engagementGateway.read(creation.getEngagementId());
        if (engagement.getClosingDate() != null) {
            throw new BadRequestException("Engagement is closed, no more invoices can be created, id: " + creation.getEngagementId());
        }
        Invoice invoice = Invoice.builder()
                .billingInfo(BillingInfo.builder()
                        .concept(creation.buildProceduresText())
                        .build()
                )
                .vatRate(DEFAULT_VAT_RATE)
                .engagement(engagement)
                .payments(this.payments(creation.getEngagementId()))
                .priorPayments(this.priorPayments(creation.getEngagementId()))
                .closed(creation.getCloseEngagement())
                .build();

        if (Boolean.TRUE.equals(creation.getCloseEngagement())) {
            List<InvoicedExpense> expenses = this.expenseGateway.findByEngagementId(creation.getEngagementId())
                    .map(this::toInvoicedExpense)
                    .toList();
            invoice.setExpenses(expenses);
            invoice.applyBaseAmount(creation.totalBudget());
            invoice.setDiscounts(engagement.getDiscounts());
            invoice.getBillingInfo()
                    .setConcept("FACTURA por cierre de Hoja de Encargos." + System.lineSeparator() + System.lineSeparator()
                            + invoice.getBillingInfo().getConcept());
        } else {
            invoice.applyTotalAmount(invoice.paymentsAmount().add(invoice.priorPaymentsAmount()));
            invoice.getBillingInfo()
                    .setConcept("FACTURA por ingreso de Provisón de Fondos." + System.lineSeparator() + System.lineSeparator()
                            + invoice.getBillingInfo().getConcept());
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
        if (Boolean.TRUE.equals(invoice.getClosed())) {
            this.engagementGateway.close(invoice.getEngagement().getId());
        }
        UserSnapshot user = this.userFinder.readById(invoice.getBillingInfo().getUserId());
        byte[] pdf = this.generatePdf(id);
        String fileName = "Ocanabogados-" + invoice.getSeries() + "-" + invoice.getNumber() + ".pdf";
        this.emailWriter.sendHtml(
                this.invoiceTemplateService.buildHtmlEmail(this.email, this.name),
                pdf,
                fileName
        );
        this.emailWriter.sendHtml(
                this.invoiceTemplateService.buildHtmlEmail(user.getEmail(), user.getFirstName()),
                pdf,
                fileName
        );

        //TODO guardar en S3 AWS
    }

    public Invoice read(UUID id) {
        Invoice invoice = this.invoiceGateway.read(id);
        if (invoice.getEngagement() != null) {
            invoice.setEngagement(this.engagementGateway.read(invoice.getEngagement().getId()));
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
                invoice.setEngagement(engagementGateway.read(engagementId));
            }
            return invoice;
        });
    }

    private void validateAndHydrate(Invoice invoice) {
        if (invoice.getEngagement() != null && invoice.getEngagement().getId() != null) {
            invoice.setEngagement(this.engagementGateway.read(invoice.getEngagement().getId()));
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
        final NumberFormat EUR = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-ES"));
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
                .title(title)
                .space()
                .paragraphBold("Nº " + invoiceNumber + "   ·   " + issueDate);

        pdf.section("FACTURAR A, CON PARTICIPACIÓN DEL : " + invoice.getPercentage() + " %")
                .paragraphBold(invoice.getBillingInfo().getFullName())
                .paragraph("N.I.F. " + invoice.getBillingInfo().getIdentity())
                .paragraph(invoice.getBillingInfo().getFullAddress());

        pdf.section("CONCEPTO")
                .paragraph(invoice.getBillingInfo().getConcept());

        pdf.section("IMPORTE DE LA PRESENTE FACTURA");
        pdf.table(
                new String[]{"Concepto", "Importe"},
                List.of(
                        new String[]{"Base imponible", EUR.format(baseAmount)},
                        new String[]{"IVA (" + vatRate.toPlainString() + "%)", EUR.format(vatAmount)}
                ),
                new String[]{"TOTAL", EUR.format(totalAmount)}
        );
        BigDecimal debt = totalAmount.subtract(invoice.paymentsAmount());
        if (debt.compareTo(new BigDecimal("4")) > 0) {
            pdf.paragraphHighlight("PENDIENTE DE INGRESAR: " + EUR.format(debt));
            pdf.paragraphHighlight("Ruego que ingrese en la cuenta bancaria: ES00 1111 2222 3333 4444 5555");
        }

        pdf.signatureLine("Doña Nuria Ocaña Pérez");

        pdf.pageBreak().section("Información detallada");

        if (invoice.getDiscounts() != null && !invoice.getDiscounts().isEmpty()) {
            pdf.paragraphBold("Descuentos aplicados a la Hoja de Encargo");
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
                            EUR.format(invoice.totalBaseAmount().add(invoice.discountsAmount())),
                            EUR.format(invoice.discountsAmount()),
                            EUR.format(invoice.totalBaseAmount())
                    }
            );
        }

        if (invoice.getPayments() != null && !invoice.getPayments().isEmpty()) {
            pdf.paragraphBold("Ingresos Asociados a la Hoja de Encargo y facturados en la presente");
            List<String[]> paymentRows = invoice.getPayments().stream()
                    .map(payment -> new String[]{
                            payment.user().toFullName(),
                            payment.date().format(DATE_FORMAT),
                            EUR.format(payment.amount()),
                            payment.method().name()
                    }).toList();
            pdf.table(
                    new String[]{"Cliente", "Fecha", "Importe", "Tipo de Ingreso"},
                    paymentRows,
                    new String[]{"TOTAL", "", EUR.format(invoice.paymentsAmount()), ""}
            );
        }

        if (invoice.getExpenses() != null && !invoice.getExpenses().isEmpty()) {
            pdf.paragraphBold("Gastos asociados a la Hoja de Encargo");
            List<String[]> expenseRows = invoice.getExpenses().stream()
                    .map(expense -> {
                        BigDecimal base = expense.baseAmount().setScale(2, RoundingMode.HALF_UP);
                        BigDecimal vat = expense.vatAmount().setScale(2, RoundingMode.HALF_UP);
                        return new String[]{
                                expense.issueDate().format(DATE_FORMAT),
                                expense.description(),
                                EUR.format(base),
                                EUR.format(vat)
                        };
                    }).toList();
            pdf.table(
                    new String[]{"Fecha", "Descripción", "Base imponible", "IVA"},
                    expenseRows,
                    new String[]{"TOTAL", "", EUR.format(invoice.expensesBaseAmount()), EUR.format(invoice.expensesVatAmount())}
            );
        }

        if (invoice.getPriorPayments() != null && !invoice.getPriorPayments().isEmpty()) {
            pdf.paragraphBold("Anteriores ingresos, Ya Facturados, de la Hoja de Encargo");
            List<String[]> paymentRows = invoice.getPriorPayments().stream()
                    .map(payment -> new String[]{
                            payment.user().toFullName(),
                            payment.date().format(DATE_FORMAT),
                            EUR.format(payment.amount()),
                            payment.method().name()
                    }).toList();
            pdf.table(
                    new String[]{"Cliente", "Fecha", "Importe", "Tipo de Ingreso"},
                    paymentRows,
                    new String[]{"TOTAL", "", EUR.format(invoice.priorPaymentsAmount()), ""}
            );
        }

        return pdf.build();
    }


}
