package es.upm.api.domain.services;

import es.upm.api.domain.model.BillingInfo;
import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.Payment;
import es.upm.api.domain.model.criteria.InvoiceFindCriteria;
import es.upm.api.domain.model.external.EngagementSnapshot;
import es.upm.api.domain.model.external.LegalProcedureSnapshot;
import es.upm.api.domain.model.external.UserSnapshot;
import es.upm.api.domain.ports.out.billing.ExpenseGateway;
import es.upm.api.domain.ports.out.billing.InvoiceGateway;
import es.upm.api.domain.ports.out.billing.PaymentGateway;
import es.upm.api.domain.ports.out.engagement.EngagementFinder;
import es.upm.api.domain.ports.out.user.UserFinder;
import es.upm.miw.exception.BadRequestException;
import es.upm.miw.exception.InvalidTransitionException;
import es.upm.miw.pdf.PdfBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
        UserSnapshot user = this.userFinder.readById(invoice.getBillingInfo().getUserId());
        invoice.getBillingInfo().updateFrom(user);
        invoice.setVatRate(DEFAULT_VAT_RATE);
        this.invoiceGateway.create(invoice);
    }

    public void createFromPayments(UUID engagementId) {
        EngagementSnapshot engagement = engagementFinder.read(engagementId);
        Map<UUID, List<Payment>> paymentsByUser = paymentGateway
                .findNotInvoicedByEngagementId(engagementId)
                .filter(payment -> payment.getUser() != null && payment.getUser().getId() != null)
                .collect(Collectors.groupingBy(p -> p.getUser().getId()));

        List<Payment> invoicedPayments = paymentGateway
                .findInvoicedByEngagementId(engagementId)
                .map(payment -> {
                    payment.setUser(userFinder.readById(payment.getUser().getId()));
                    return payment;
                })
                .toList();

        paymentsByUser.forEach((userId, userPayments) -> {
            List<Payment> userInvoicedPayments = invoicedPayments.stream()
                    .filter(payment -> payment.getUser() != null)
                    .filter(payment -> userId.equals(payment.getUser().getId()))
                    .toList();
            createInvoiceFor(userId, userPayments, engagement, userInvoicedPayments);
            markAsInvoiced(userPayments);
        });
    }

    private void createInvoiceFor(UUID userId, List<Payment> payments,
                                  EngagementSnapshot engagement, List<Payment> invoicedPayments) {
        BigDecimal grossAmount = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal divisor = BigDecimal.ONE.add(DEFAULT_VAT_RATE
                .divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP));
        BigDecimal baseAmount = grossAmount.divide(divisor, 4, RoundingMode.HALF_UP);
        String procedures = engagement.getLegalProcedures() == null ? ""
                : engagement.getLegalProcedures().stream()
                .map(LegalProcedureSnapshot::getTitle)
                .collect(Collectors.joining(", "));
        String engagementDate = engagement.getLastUpdatedDate() == null
                ? LocalDate.now().format(DATE_FORMAT)
                : engagement.getLastUpdatedDate().format(DATE_FORMAT);
        Invoice invoice = Invoice.builder()
                .billingInfo(BillingInfo.builder()
                        .userId(userId)
                        .concept(String
                                .format("Provisión de fondos.%nHoja de encargo aceptada el %s.%nProcedimientos legales: %s.",
                                        engagementDate, procedures))
                        .build())
                .engagement(engagement)
                .payments(payments)
                .invoicedPayments(invoicedPayments)
                .baseAmount(baseAmount)
                .build();
        this.create(invoice);
    }

    private void markAsInvoiced(List<Payment> payments) {
        payments.forEach(payment -> {
            payment.setInvoiced(true);
            paymentGateway.update(payment.getId(), payment);
        });
    }

    public void createFromEngagement(UUID engagementId, BigDecimal totalBaseAmount, String concept) {
        EngagementSnapshot engagement = this.engagementFinder.read(engagementId);
        List<Expense> expenses = this.expenseGateway.findByEngagementId(engagementId).toList();
        List<Payment> invoicedPayments = this.paymentGateway
                .findInvoicedByEngagementId(engagementId)
                .map(payment -> {
                    payment.setUser(this.userFinder.readById(payment.getUser().getId()));
                    return payment;
                })
                .toList();
        BigDecimal invoicedTotalAmount = invoicedPayments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal divisor = BigDecimal.ONE.add(DEFAULT_VAT_RATE
                .divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP));
        BigDecimal invoicedBaseAmount = invoicedTotalAmount.divide(divisor, 4, RoundingMode.HALF_UP);
        BigDecimal pendingServiceBaseAmount = totalBaseAmount
                .subtract(invoicedBaseAmount)
                .setScale(4, RoundingMode.HALF_UP);
        if (pendingServiceBaseAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Pending service base amount must be greater than zero");
        }
        Invoice invoice = Invoice.builder()
                .billingInfo(BillingInfo.builder()
                        .userId(engagement.getOwner().getId())
                        .concept(concept)
                        .build())
                .engagement(engagement)
                .invoicedPayments(invoicedPayments)
                .expenses(expenses)
                .baseAmount(pendingServiceBaseAmount)
                .build();
        this.create(invoice);

    }


    public void emission(UUID id) {
        Invoice invoice = this.read(id);
        if (invoice.getEmissionDate() != null) {
            throw new IllegalStateException("Already issued");
        }
        String series = String.valueOf(LocalDate.now().getYear());
        invoice.setSeries(series);
        invoice.setNumber(invoiceGateway.findNextNumber(series));
        invoice.setEmissionDate(LocalDate.now());
        this.invoiceGateway.update(id, invoice);
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
            List<Payment> hydratedPayments = invoice.getPayments().stream()
                    .map(payment -> this.paymentGateway.read(payment.getId()))
                    .toList();
            invoice.setPayments(hydratedPayments);

            BigDecimal paymentsTotal = hydratedPayments.stream()
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal discountsTotal = invoice.getDiscounts() == null ? BigDecimal.ZERO
                    : invoice.getDiscounts().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            invoice.setBaseAmount(paymentsTotal.subtract(discountsTotal));
        }
        if (invoice.getVatRate() == null) {
            invoice.setVatRate(DEFAULT_VAT_RATE);
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

        BigDecimal vatRate = invoice.getVatRate() == null ? DEFAULT_VAT_RATE : invoice.getVatRate();
        BigDecimal serviceBaseAmount = invoice.getBaseAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal serviceVatAmount = invoice.getServiceVatAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal expensesBaseAmount = invoice.getExpensesBaseAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal expensesVatAmount = invoice.getExpensesVatAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalBaseAmount = invoice.getTotalBaseAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalVatAmount = invoice.getTotalVatAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = invoice.getTotalAmount().setScale(2, RoundingMode.HALF_UP);

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

        if (invoice.getPayments() != null && !invoice.getPayments().isEmpty()) {
            pdf.section("INGRESOS");
            List<String[]> paymentRows = invoice.getPayments().stream()
                    .map(p -> new String[]{
                            p.getDate().format(DATE_FORMAT),
                            p.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString() + " €",
                            p.getMethod().name()
                    })
                    .toList();
            pdf.table(new String[]{"Fecha", "Importe", "Tipo de Ingreso"}, paymentRows);
        }

        pdf.section("IMPORTES DE LA PRESENTE FACTURA");
        List<String[]> amountRows = List.of(
                new String[]{"Base servicios", serviceBaseAmount.toPlainString() + " €"},
                new String[]{"IVA servicios (" + vatRate.toPlainString() + "%)", serviceVatAmount.toPlainString() + " €"},
                new String[]{"Base gastos", expensesBaseAmount.toPlainString() + " €"},
                new String[]{"IVA gastos", expensesVatAmount.toPlainString() + " €"},
                new String[]{"BASE TOTAL", totalBaseAmount.toPlainString() + " €"},
                new String[]{"IVA TOTAL", totalVatAmount.toPlainString() + " €"},
                new String[]{"TOTAL", totalAmount.toPlainString() + " €"}
        );
        pdf.table(new String[]{"Concepto", "Importe"}, amountRows);

        if (invoice.getExpenses() != null && !invoice.getExpenses().isEmpty()) {
            pdf.section("GASTOS ASOCIADOS AL ENCARGO");

            List<String[]> expenseRows = invoice.getExpenses().stream()
                    .map(e -> {
                        BigDecimal base = e.getBaseAmount().setScale(2, RoundingMode.HALF_UP);
                        BigDecimal vat = base.multiply(BigDecimal.valueOf(e.getVatRate()))
                                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                        return new String[]{
                                e.getIssueDate().format(DATE_FORMAT),
                                e.getDescription(),
                                base.toPlainString() + " €",
                                vat.toPlainString() + " €"
                        };
                    })
                    .toList();

            pdf.table(new String[]{"Fecha", "Descripción", "Base imponible", "IVA"}, expenseRows);
        }

        if (invoice.getInvoicedPayments() != null && !invoice.getInvoicedPayments().isEmpty()) {
            pdf.section("ANTERIORES INGRESOS YA FACTURADOS");
            List<String[]> paymentRows = invoice.getInvoicedPayments().stream()
                    .map(p -> new String[]{
                            p.getUser().toFullName(),
                            p.getDate().format(DATE_FORMAT),
                            p.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString() + " €",
                            p.getMethod().name()
                    })
                    .toList();
            pdf.table(new String[]{"User", "Fecha", "Importe", "Tipo de Ingreso"}, paymentRows);
        }



        pdf.space(3)
                .signatureLine("Doña Nuria Ocaña Pérez");

        return pdf.build();
    }


}
