package es.upm.api.adapter.out.billing.mongo.repositories;

import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceEntity;
import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceRepository;
import es.upm.api.domain.model.BillingInfo;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.Payment;
import es.upm.api.domain.model.PaymentMethod;
import es.upm.api.domain.model.external.EngagementSnapshot;
import es.upm.api.domain.model.external.UserSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataMongoTest
@ActiveProfiles("test")
class InvoiceRepositoryTest {

    @Autowired
    private InvoiceRepository invoiceRepository;

    private Invoice invoice;

    @BeforeEach
    void setUp() {
        this.invoiceRepository.deleteAll();
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
                        .build()))
                .discounts(List.of(BigDecimal.TEN))
                .pdfPath("/tmp/invoice.pdf")
                .build();
    }

    @Test
    void shouldSaveInvoice() {
        InvoiceEntity saved = this.invoiceRepository.save(new InvoiceEntity(this.invoice));

        assertNotNull(saved);
        assertEquals(this.invoice.getId(), saved.getId());
        assertEquals(this.invoice.getEngagement().getId(), saved.getEngagementId());
        assertEquals(this.invoice.getBillingInfo(), saved.getBillingInfo());
        assertEquals(this.invoice.getPayments(), saved.getPayments());
    }

    @Test
    void shouldFindInvoiceById() {
        InvoiceEntity saved = this.invoiceRepository.save(new InvoiceEntity(this.invoice));
        Optional<InvoiceEntity> optional = this.invoiceRepository.findById(saved.getId());

        assertTrue(optional.isPresent());
        assertEquals(saved.getId(), optional.get().getId());
        assertEquals(saved.getEngagementId(), optional.get().getEngagementId());
        assertEquals(saved.getEmissionDate(), optional.get().getEmissionDate());
    }

    @Test
    void shouldFindInvoiceByEmissionDateFrom() {
        InvoiceEntity saved = this.invoiceRepository.save(new InvoiceEntity(this.invoice));

        List<InvoiceEntity> all = this.invoiceRepository.findAllByOrderByEmissionDateDesc();
        List<InvoiceEntity> byDate = this.invoiceRepository.findByEmissionDateGreaterThanEqualOrderByEmissionDateDesc(
                saved.getEmissionDate()
        );

        assertEquals(1, all.size());
        assertEquals(saved.getId(), all.get(0).getId());
        assertEquals(1, byDate.size());
        assertEquals(saved.getId(), byDate.get(0).getId());
    }
}
