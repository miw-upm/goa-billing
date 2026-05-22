package es.upm.api.domain.services;

import es.upm.api.domain.model.BillingInfo;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.Payment;
import es.upm.api.domain.model.criteria.InvoiceFindCriteria;
import es.upm.api.domain.ports.out.billing.InvoiceGateway;
import es.upm.api.domain.ports.out.billing.PaymentGateway;
import es.upm.api.domain.ports.out.engagement.EngagementFinder;
import es.upm.api.domain.ports.out.user.UserFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    private final EngagementFinder engagementFinder;
    private final UserFinder userFinder;

    public Invoice create(Invoice invoice) {
        invoice.setId(UUID.randomUUID());
        invoice.setEmissionDate(LocalDate.now());
        this.validateAndHydrate(invoice);
        this.invoiceGateway.create(invoice);
        return invoice;
    }

    public Invoice update(UUID id, Invoice invoice) {
        Invoice currentInvoice = this.invoiceGateway.read(id);
        invoice.setId(id);
        invoice.setEmissionDate(currentInvoice.getEmissionDate());
        invoice.setPdfPath(currentInvoice.getPdfPath());
        this.validateAndHydrate(invoice);
        return this.invoiceGateway.update(id, invoice);
    }

    public Invoice read(UUID id) {
        Invoice invoice = this.invoiceGateway.read(id);
        invoice.setEngagement(this.engagementFinder.read(invoice.getEngagement().getEngagementId()));
        invoice.setBillingInfo(this.hydrateBillingInfo(invoice.getBillingInfo()));
        return invoice;
    }

    public void delete(UUID id) {
        this.invoiceGateway.delete(id);
    }

    public Stream<Invoice> find(InvoiceFindCriteria criteria) {
        return this.invoiceGateway.find(criteria);
    }

    private void validateAndHydrate(Invoice invoice) {
        invoice.setEngagement(this.engagementFinder.read(invoice.getEngagement().getEngagementId()));
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
        return BillingInfo.from(this.userFinder.readById(billingInfo.getUserId()));
    }
}
