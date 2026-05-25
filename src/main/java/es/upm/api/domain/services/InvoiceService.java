package es.upm.api.domain.services;

import es.upm.api.domain.model.BillingInfo;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.Payment;
import es.upm.api.domain.model.criteria.InvoiceFindCriteria;
import es.upm.api.domain.model.external.EngagementSnapshot;
import es.upm.api.domain.model.external.LegalProcedureSnapshot;
import es.upm.api.domain.model.external.UserSnapshot;
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
import java.util.HashMap;
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
                .collect(Collectors.groupingBy(p -> p.getUser().getId()));
        paymentsByUser.forEach((userId, userPayments) -> {
            createInvoiceFor(userId, userPayments, engagement);
            markAsInvoiced(userPayments);
        });
    }
    private void createInvoiceFor(UUID userId, List<Payment> payments,
                                  EngagementSnapshot engagement) {
        BigDecimal grossAmount = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal baseAmount = this.baseAmountFromGross(grossAmount, DEFAULT_VAT_RATE);
        String procedures = engagement.getLegalProcedures().stream()
                .map(LegalProcedureSnapshot::getTitle)
                .collect(Collectors.joining(", "));
        String engagementDate = engagement.getLastUpdatedDate().format(DATE_FORMAT);
        Invoice invoice = Invoice.builder()
                .billingInfo(BillingInfo.builder()
                        .userId(userId)
                        .concept(String.format("Provisión de fondos.%nHoja de encargo aceptada el %s.%nProcedimientos legales: %s",
                                engagementDate, procedures))
                        .build())
                .engagement(engagement)
                .payments(payments)
                .baseAmount(baseAmount)
                .build();
        this.create(invoice);
    }

    private BigDecimal baseAmountFromGross(BigDecimal grossAmount, BigDecimal vatRate) {
        long grossCents = grossAmount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        BigDecimal divisor = BigDecimal.ONE.add(vatRate.divide(new BigDecimal("100"), 12, RoundingMode.HALF_UP));
        long candidateBaseCents = grossAmount.divide(divisor, 2, RoundingMode.HALF_UP)
                .movePointRight(2).longValueExact();

        for (long delta = 0; delta <= grossCents; delta++) {
            long up = candidateBaseCents + delta;
            if (this.totalCentsFromBaseCents(up, vatRate) == grossCents) {
                return BigDecimal.valueOf(up, 2);
            }
            long down = candidateBaseCents - delta;
            if (down >= 0 && this.totalCentsFromBaseCents(down, vatRate) == grossCents) {
                return BigDecimal.valueOf(down, 2);
            }
        }

        BigDecimal baseAmount = BigDecimal.valueOf(candidateBaseCents, 2);
        BigDecimal totalAmount = this.totalAmountFromBase(baseAmount, vatRate);
        if (totalAmount.compareTo(grossAmount) > 0) {
            return baseAmount.subtract(new BigDecimal("0.01"));
        }
        if (totalAmount.compareTo(grossAmount) < 0) {
            return baseAmount.add(new BigDecimal("0.01"));
        }
        return baseAmount;
    }

    private long totalCentsFromBaseCents(long baseCents, BigDecimal vatRate) {
        BigDecimal baseAmount = BigDecimal.valueOf(baseCents, 2);
        BigDecimal vatAmount = baseAmount.multiply(vatRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = baseAmount.add(vatAmount).setScale(2, RoundingMode.HALF_UP);
        return totalAmount.movePointRight(2).longValueExact();
    }

    private BigDecimal totalAmountFromBase(BigDecimal baseAmount, BigDecimal vatRate) {
        BigDecimal vatAmount = baseAmount.multiply(vatRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        return baseAmount.add(vatAmount).setScale(2, RoundingMode.HALF_UP);
    }

    private void markAsInvoiced(List<Payment> payments) {
        payments.forEach(payment -> {
            payment.setInvoiced(true);
            paymentGateway.update(payment.getId(), payment);
        });
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

    public Stream<Invoice> find(InvoiceFindCriteria criteria) { //TODO No tengo claro las queries utiles
        Stream<Invoice> invoices = this.invoiceGateway.find(criteria);
        if (criteria == null || !StringUtils.hasText(criteria.getClient())) {
            return invoices;
        }
        List<UUID> clientIds = this.userFinder.find(criteria.getClient()).stream()
                .map(UserSnapshot::getId)
                .toList();
        return invoices.filter(invoice -> invoice.getBillingInfo() != null
                && invoice.getBillingInfo().getUserId() != null
                && clientIds.contains(invoice.getBillingInfo().getUserId()));
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
        BigDecimal baseAmount = invoice.getBaseAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal vatAmount = baseAmount.multiply(vatRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = baseAmount.add(vatAmount).setScale(2, RoundingMode.HALF_UP);

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

        pdf.section("IMPORTES");
        List<String[]> amountRows = List.of(
                new String[]{"Base imponible", baseAmount.toPlainString() + " €"},
                new String[]{"IVA (" + vatRate.toPlainString() + "%)", vatAmount.toPlainString() + " €"},
                new String[]{"TOTAL", totalAmount.toPlainString() + " €"}
        );
        pdf.table(new String[]{"Concepto", "Importe"}, amountRows);

        pdf.space(3)
                .signatureLine("Doña Nuria Ocaña Pérez");

        return pdf.build();
    }


}
