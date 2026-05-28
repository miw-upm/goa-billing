package es.upm.api.domain.services;

import es.upm.api.domain.model.BillingInfo;
import es.upm.api.domain.model.InvoicedExpense;
import es.upm.api.domain.model.InvoicedPayment;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.Payment;
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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class InvoiceService {
    private static final BigDecimal DEFAULT_VAT_RATE = new BigDecimal("21");
    private final InvoiceGateway invoiceGateway;
    private final PaymentGateway paymentGateway;
    private final ExpenseGateway expenseGateway;
    private final EngagementGateway engagementGateway;
    private final UserFinder userFinder;
    private final EmailWriter emailWriter;
    private final InvoiceTemplateService invoiceTemplateService;
    private final InvoicePdfService invoicePdfService;
    @Value("${app.administration.name}")
    private String name;
    @Value("${app.administration.email}")
    private String email;

    public void create(Invoice invoice) {
        invoice.setId(UUID.randomUUID());
        invoice.setPercentage(new BigDecimal("100"));
        invoice.setBillingInfo(new BillingInfo(this.userFinder.readById(invoice.getBillingInfo().getUserId())));
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

    private List<InvoicedExpense> expenses(UUID engagementId) {
        return this.expenseGateway.findByEngagementId(engagementId)
                .map(InvoicedExpense::new)
                .toList();
    }

    public void createFromEngagement(InvoiceCreationFromEngagement creation) {
        EngagementSnapshot engagement = this.engagementGateway.read(creation.getEngagementId());
        if (engagement.getClosingDate() != null) {
            throw new BadRequestException("Engagement is closed, no more invoices can be created, id: " + creation.getEngagementId());
        }
        Invoice invoiceTemplate = Invoice.builder()
                .concept(creation.getConcept())
                .closed(creation.getCloseEngagement())
                .vatRate(DEFAULT_VAT_RATE)
                .engagement(engagement)
                .legalProcedures(creation.getLegalProcedures())
                .payments(this.payments(creation.getEngagementId()))
                .priorPayments(this.priorPayments(creation.getEngagementId()))
                .build();

        if (Boolean.TRUE.equals(invoiceTemplate.getClosed())) {
            invoiceTemplate.setExpenses(this.expenses(engagement.getId()));
            invoiceTemplate.applyBaseAmount(invoiceTemplate.totalBudget());
            invoiceTemplate.setDiscounts(engagement.getDiscounts());
        } else {
            invoiceTemplate.applyTotalAmount(invoiceTemplate.paymentsAmount().add(invoiceTemplate.priorPaymentsAmount()));
        }
        creation.getBillingPercentages().stream()
                .filter(userPercentage -> userPercentage.getPercentage().compareTo(BigDecimal.ZERO) > 0)
                .forEach(userPercentage -> {
                    invoiceTemplate.setId(UUID.randomUUID());
                    invoiceTemplate.setBillingInfo(new BillingInfo(this.userFinder.readById(userPercentage.getUserId())));
                    invoiceTemplate.setPercentage(userPercentage.getPercentage());
                    this.invoiceGateway.create(invoiceTemplate);
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

    public byte[] generatePdf(UUID id) {
        Invoice invoice = this.read(id);
        return this.invoicePdfService.generatePdf(invoice);
    }
}
