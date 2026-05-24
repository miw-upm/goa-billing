package es.upm.api.domain.services;

import es.upm.api.domain.model.BillingInfo;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.Payment;
import es.upm.api.domain.model.criteria.InvoiceFindCriteria;
import es.upm.api.domain.model.external.UserSnapshot;
import es.upm.api.domain.ports.out.billing.InvoiceGateway;
import es.upm.api.domain.ports.out.billing.PaymentGateway;
import es.upm.api.domain.ports.out.engagement.EngagementFinder;
import es.upm.api.domain.ports.out.user.UserFinder;
import es.upm.miw.exception.InvalidTransitionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private static final BigDecimal DEFAULT_VAT_RATE = new BigDecimal("21");

    private final InvoiceGateway invoiceGateway;
    private final PaymentGateway paymentGateway;
    private final EngagementFinder engagementFinder;
    private final UserFinder userFinder;

    public void create(Invoice invoice) {
        invoice.setId(UUID.randomUUID());
        invoice.setBillingInfo(BillingInfo.from(this.userFinder.readById(invoice.getBillingInfo().getUserId())));
        this.invoiceGateway.create(invoice);
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
        this.invoiceGateway.findById(id)
                .ifPresent(invoice -> {
                    if (invoice.getEmissionDate() != null) {
                        throw new InvalidTransitionException("Issued invoices cannot be deleted, id: " + id);
                    }
                    this.invoiceGateway.delete(id);
                });
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
        BillingInfo hydratedBillingInfo = BillingInfo.from(this.userFinder.readById(billingInfo.getUserId()));
        hydratedBillingInfo.setConcept(billingInfo.getConcept());
        return hydratedBillingInfo;
    }
}
