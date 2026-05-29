package es.upm.api.adapter.out.billing.mongo.repositories;

import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceEntity;
import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceRepository;
import es.upm.api.domain.model.*;
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

import static org.junit.jupiter.api.Assertions.*;

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
                .vatAmount(BigDecimal.valueOf(18.9))
                .vatRate(BigDecimal.valueOf(21))
                .engagement(EngagementSnapshot.builder().id(engagementId).build())
                .payments(List.of(new InvoicedPayment(
                        paymentId,
                        LocalDate.of(2026, 3, 18),
                        BigDecimal.valueOf(100),
                        PaymentMethod.TRANSFER,
                        UserSnapshot.builder().id(userId).build()
                )))
                .priorPayments(List.of(new InvoicedPayment(
                        UUID.randomUUID(),
                        LocalDate.of(2026, 3, 10),
                        BigDecimal.valueOf(80),
                        PaymentMethod.CASH,
                        UserSnapshot.builder().id(userId).build()
                )))
                .expenses(List.of(new InvoicedExpense(
                        UUID.randomUUID(),
                        LocalDate.of(2026, 3, 11),
                        "gasto",
                        BigDecimal.valueOf(15),
                        BigDecimal.valueOf(1.5)
                )))
                .discounts(List.of(BigDecimal.TEN))
                .pdfPath("/tmp/invoice.pdf")
                .build();
    }

    @Test
    void shouldSaveInvoice() {
        InvoiceEntity saved = this.invoiceRepository.save(new InvoiceEntity(this.invoice));

        assertNotNull(saved);
        assertEquals(this.invoice.getId().toString(), saved.getId());
        assertEquals(this.invoice.getEngagement().getId().toString(), saved.getEngagementId());
        assertEquals(this.invoice.getBillingInfo(), saved.getBillingInfo().toDomain());
        assertEquals(this.invoice.getPayments(), saved.getPayments().stream().map(paymentEntity -> new InvoicedPayment(paymentEntity.toDomain())).toList());
        assertEquals(this.invoice.getPriorPayments(), saved.getPriorPayments().stream().map(paymentEntity -> new InvoicedPayment(paymentEntity.toDomain())).toList());
        assertEquals(this.invoice.getExpenses(), saved.toDomain().getExpenses());
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
