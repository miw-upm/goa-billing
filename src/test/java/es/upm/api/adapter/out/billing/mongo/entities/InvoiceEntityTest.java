package es.upm.api.adapter.out.billing.mongo.entities;

import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceEntity;
import es.upm.api.domain.model.BillingInfo;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.Payment;
import es.upm.api.domain.model.PaymentMethod;
import es.upm.api.domain.model.external.EngagementSnapshot;
import es.upm.api.domain.model.external.UserSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvoiceEntityTest {

    private Invoice invoice;

    @BeforeEach
    void setUp() {
        UUID engagementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        this.invoice = Invoice.builder()
                .id(UUID.randomUUID())
                .billingInfo(BillingInfo.builder()
                        .userId(userId)
                        .fullName("John Doe")
                        .identity("12345678A")
                        .fullAddress("Madrid")
                        .build())
                .emissionDate(LocalDate.of(2026, 3, 20))
                .operationDate(LocalDate.of(2026, 3, 19))
                .series("A")
                .number(1)
                .baseAmount(BigDecimal.valueOf(90))
                .vatRate(BigDecimal.valueOf(21))
                .engagement(EngagementSnapshot.builder().id(engagementId).build())
                .payments(List.of(Payment.builder()
                        .id(paymentId)
                        .engagement(EngagementSnapshot.builder().id(engagementId).build())
                        .user(UserSnapshot.builder().id(userId).build())
                        .amount(BigDecimal.valueOf(100))
                        .method(PaymentMethod.TRANSFER)
                        .date(LocalDate.of(2026, 3, 18))
                        .invoiced(false)
                        .build()))
                .invoicedPayments(List.of(Payment.builder()
                        .id(UUID.randomUUID())
                        .engagement(EngagementSnapshot.builder().id(engagementId).build())
                        .user(UserSnapshot.builder().id(userId).build())
                        .amount(BigDecimal.valueOf(50))
                        .method(PaymentMethod.CASH)
                        .date(LocalDate.of(2026, 3, 10))
                        .invoiced(true)
                        .build()))
                .discounts(List.of(BigDecimal.TEN))
                .pdfPath("/tmp/invoice.pdf")
                .build();
    }

    @Test
    void shouldBuildInvoiceEntityFromInvoice() {
        InvoiceEntity entity = new InvoiceEntity(this.invoice);

        assertEquals(this.invoice.getId(), entity.getId());
        assertEquals(this.invoice.getBillingInfo(), entity.getBillingInfo());
        assertEquals(this.invoice.getEmissionDate(), entity.getEmissionDate());
        assertEquals(this.invoice.getOperationDate(), entity.getOperationDate());
        assertEquals(this.invoice.getSeries(), entity.getSeries());
        assertEquals(this.invoice.getNumber(), entity.getNumber());
        assertEquals(this.invoice.getBaseAmount(), entity.getBaseAmount());
        assertEquals(this.invoice.getVatRate(), entity.getVatRate());
        assertEquals(this.invoice.getEngagement().getId(), entity.getEngagementId());
        assertEquals(this.invoice.getPayments(), entity.getPayments());
        assertEquals(this.invoice.getInvoicedPayments(), entity.getInvoicedPayments());
        assertEquals(this.invoice.getDiscounts(), entity.getDiscounts());
        assertEquals(this.invoice.getPdfPath(), entity.getPdfPath());
    }

    @Test
    void shouldConvertInvoiceEntityToDomain() {
        InvoiceEntity entity = new InvoiceEntity(this.invoice);

        Invoice mapped = entity.toDomain();

        assertEquals(entity.getId(), mapped.getId());
        assertEquals(entity.getBillingInfo(), mapped.getBillingInfo());
        assertEquals(entity.getEmissionDate(), mapped.getEmissionDate());
        assertEquals(entity.getOperationDate(), mapped.getOperationDate());
        assertEquals(entity.getSeries(), mapped.getSeries());
        assertEquals(entity.getNumber(), mapped.getNumber());
        assertEquals(entity.getBaseAmount(), mapped.getBaseAmount());
        assertEquals(entity.getVatRate(), mapped.getVatRate());
        assertEquals(entity.getEngagementId(), mapped.getEngagement().getId());
        assertEquals(entity.getPayments(), mapped.getPayments());
        assertEquals(entity.getInvoicedPayments(), mapped.getInvoicedPayments());
        assertEquals(entity.getDiscounts(), mapped.getDiscounts());
        assertEquals(entity.getPdfPath(), mapped.getPdfPath());
    }
}
