package es.upm.api.domain.services;

import es.upm.api.domain.model.*;
import es.upm.api.domain.model.creation.InvoiceCreationFromEngagement;
import es.upm.api.domain.model.creation.InvoiceCreationRectification;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class InvoiceService {
    private static final int SCALE = 6;
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
        invoice.setBillingInfo(new BillingInfo(this.userFinder.readById(invoice.getBillingInfo().getUserId())));
        invoice.applyVatRate(DEFAULT_VAT_RATE);
        this.invoiceGateway.create(invoice);
    }

    private List<Payment> priorPayments(UUID engagementId) {
        return this.paymentGateway
                .findInvoicedByEngagementId(engagementId)
                .map(payment -> {
                    payment.setUser(this.userFinder.readById(payment.getUser().getId()));
                    return payment;
                })
                .toList();
    }

    private List<Payment> payments(UUID engagementId) {
        return this.paymentGateway
                .findNotInvoicedByEngagementId(engagementId)
                .map(payment -> {
                    payment.setUser(this.userFinder.readById(payment.getUser().getId()));
                    return payment;
                })
                .toList();
    }

    private List<Expense> expenses(UUID engagementId) {
        return this.expenseGateway.findByEngagementId(engagementId)
                .toList();
    }

    public void createFromEngagement(InvoiceCreationFromEngagement creation) {
        EngagementSnapshot engagement = this.engagementGateway.read(creation.getEngagementId());
        if (engagement.getClosingDate() != null) {
            throw new BadRequestException("Engagement is closed, no more invoices can be created, id: " + creation.getEngagementId());
        }
        Invoice invoice = Invoice.builder()
                .concept(creation.getConcept())
                .closed(creation.getCloseEngagement())
                .vatRate(DEFAULT_VAT_RATE)
                .engagement(engagement)
                .legalProcedures(creation.getLegalProcedures())
                .payments(this.payments(creation.getEngagementId()))
                .priorPayments(this.priorPayments(creation.getEngagementId()))
                .build();
        if (Boolean.TRUE.equals(invoice.getClosed())) {
            invoice.setExpenses(this.expenses(engagement.getId()));
            invoice.setDiscounts(engagement.getDiscounts());
        }

        creation.getBillingPercentages().stream()
                .filter(userPercentage -> userPercentage.getPercentage().compareTo(BigDecimal.ZERO) > 0)
                .forEach(userPercentage -> {
                    invoice.setId(UUID.randomUUID());
                    invoice.setPercentage(userPercentage.getPercentage());
                    if (Boolean.TRUE.equals(invoice.getClosed())) {
                        BigDecimal base = invoice.totalBudget()
                                .subtract(invoice.discountsAmount())
                                .subtract(invoice.priorPaymentsBaseAmount())
                                .multiply(invoice.percentageFactor());
                        invoice.applyBaseAmount(base);
                        invoice.setBaseExpense(invoice.expensesBaseAmount().multiply(invoice.percentageFactor()));
                        invoice.setVatExpense(invoice.expensesVatAmount().multiply(invoice.percentageFactor()));
                    } else {
                        BigDecimal total = invoice.paymentsAmount()
                                .multiply(invoice.percentageFactor());
                        if (total.compareTo(BigDecimal.ZERO) == 0) {
                            throw new InvalidTransitionException("An invoice with a zero amount is not valid.");
                        }
                        invoice.applyTotalAmount(total);
                        invoice.setBaseExpense(BigDecimal.ZERO);
                        invoice.setVatExpense(BigDecimal.ZERO);
                    }
                    invoice.setBillingInfo(new BillingInfo(this.userFinder.readById(userPercentage.getUserId())));
                    invoice.setPercentage(userPercentage.getPercentage());
                    this.invoiceGateway.create(invoice);
                });
    }

    public void emission(UUID id) {
        Invoice invoice = this.read(id);
        if (invoice.getEmissionDate() != null) {
            throw new IllegalStateException("Already invoice issued: " + id);
        }
        if (invoice.getNumber() == null) {

        }
        String series = String.valueOf(LocalDate.now().getYear());
        invoice.setSeries(series);
        invoice.setNumber(invoiceGateway.findNextNumber(series));
        invoice.setEmissionDate(LocalDate.now());
        this.invoiceGateway.update(id, invoice);
        if (invoice.getPayments() != null) {
            invoice.getPayments().forEach(invoicePayment -> {
                Payment payment = this.paymentGateway.read(invoicePayment.getId());
                payment.setInvoiced(true);
                paymentGateway.update(invoicePayment.getId(), payment);
            });
        }
        if (Boolean.TRUE.equals(invoice.getClosed())) {
            this.engagementGateway.close(invoice.getEngagement().getId());
        }
        UserSnapshot user = this.userFinder.readById(invoice.getBillingInfo().getUserId());
        byte[] pdf = this.invoicePdfService.generatePdf(invoice, true);
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

    public Invoice createRectification(@Valid InvoiceCreationRectification creation) {
        Invoice originalInvoice = this.invoiceGateway.read(creation.getSeries(), creation.getNumber());
        if (originalInvoice.getEmissionDate() == null) {
            throw new BadRequestException("Cannot rectify a draft invoice: " + creation.getSeries() + "-" + creation.getNumber());
        }
        Invoice invoice = Invoice.builder()
                .id(UUID.randomUUID())
                .originalInvoice(OriginalInvoice.builder()
                        .series(creation.getSeries())
                        .number(creation.getNumber())
                        .emissionDate(originalInvoice.getEmissionDate())
                        .reason(creation.getReason())
                        .build())
                .concept(originalInvoice.buildConceptString())
                .billingInfo(originalInvoice.getBillingInfo())
                .percentage(originalInvoice.getPercentage())
                .operationDate(originalInvoice.getOperationDate())
                .baseAmount(originalInvoice.getBaseAmount())
                .vatAmount(originalInvoice.getVatAmount())
                .vatRate(originalInvoice.getVatRate())
                .baseExpense(originalInvoice.getBaseExpense())
                .vatExpense(originalInvoice.getVatExpense())
                .build();

        return this.invoiceGateway.create(invoice);
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
            if (invoice.getEngagement() != null
                    && invoice.getEngagement().getId() != null
                    && !StringUtils.hasText(invoice.getEngagement().getReference())) {
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
                    .map(Payment::getAmount)
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
                    .divide(new BigDecimal("100"), SCALE, RoundingMode.HALF_UP));
        }
    }

    public byte[] generatePdf(UUID id) {
        Invoice invoice = this.read(id);
        return this.invoicePdfService.generatePdf(invoice, false);
    }


}
