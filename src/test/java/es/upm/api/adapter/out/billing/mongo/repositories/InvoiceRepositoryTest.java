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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataMongoTest
@ActiveProfiles("test")
class InvoiceRepositoryTest {

    @Autowired
    private InvoiceRepository invoiceRepository;

    private Invoice firstInvoice;
    private Invoice secondInvoice;

    @BeforeEach
    void setUp() {
        this.invoiceRepository.deleteAll();
        UUID engagementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        this.firstInvoice = Invoice.builder()
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
                .payments(List.of(Payment.builder()
                        .id(paymentId)
                        .date(LocalDate.of(2026, 3, 18))
                        .amount(BigDecimal.valueOf(100))
                        .method(PaymentMethod.TRANSFER)
                        .user(UserSnapshot.builder().id(userId).build())
                        .build()))
                .priorPayments(List.of(Payment.builder()
                        .id(UUID.randomUUID())
                        .date(LocalDate.of(2026, 3, 10))
                        .amount(BigDecimal.valueOf(80))
                        .method(PaymentMethod.CASH)
                        .user(UserSnapshot.builder().id(userId).build())
                        .build()))
                .expenses(List.of(Expense.builder()
                        .id(UUID.randomUUID())
                        .issueDate(LocalDate.of(2026, 3, 11))
                        .description("gasto")
                        .baseAmount(BigDecimal.valueOf(15))
                        .vatRate(10)
                        .expenseType(ExpenseType.CURRENT)
                        .build()))
                .discounts(List.of(BigDecimal.TEN))
                .pdfPath("/tmp/invoice.pdf")
                .build();
        this.secondInvoice = Invoice.builder()
                .id(UUID.randomUUID())
                .billingInfo(BillingInfo.builder()
                        .userId(userId)
                        .fullName("John Doe")
                        .identity("12345678A")
                        .fullAddress("Madrid")
                        .build())
                .emissionDate(LocalDate.of(2026, 3, 25))
                .operationDate(LocalDate.of(2026, 3, 24))
                .series("A")
                .number(2)
                .baseAmount(BigDecimal.valueOf(120))
                .vatAmount(BigDecimal.valueOf(25.2))
                .vatRate(BigDecimal.valueOf(21))
                .engagement(EngagementSnapshot.builder().id(engagementId).build())
                .payments(List.of(Payment.builder()
                        .id(UUID.randomUUID())
                        .date(LocalDate.of(2026, 3, 24))
                        .amount(BigDecimal.valueOf(120))
                        .method(PaymentMethod.BIZUM)
                        .user(UserSnapshot.builder().id(userId).build())
                        .build()))
                .priorPayments(List.of())
                .expenses(List.of())
                .discounts(List.of())
                .pdfPath("/tmp/invoice-2.pdf")
                .build();
        this.invoiceRepository.saveAll(List.of(new InvoiceEntity(this.firstInvoice), new InvoiceEntity(this.secondInvoice)));
    }

    @Test
    void shouldFindByEmissionDateGreaterThanEqualOrderByEmissionDateDesc() {
        List<InvoiceEntity> byDate = this.invoiceRepository.findByEmissionDateGreaterThanEqualOrderByEmissionDateDesc(
                LocalDate.of(2026, 3, 21)
        );

        assertEquals(1, byDate.size());
        assertEquals(this.secondInvoice.getId().toString(), byDate.getFirst().getId());
    }

    @Test
    void shouldFindByEngagementIdPrefixOrderByEmissionDateDesc() {
        String engagementPrefix = this.firstInvoice.getEngagement().getId().toString().substring(0, 4);
        List<InvoiceEntity> result = this.invoiceRepository.findByEngagementIdStartingWithOrderByEmissionDateDesc(engagementPrefix);

        assertEquals(2, result.size());
        assertEquals(this.secondInvoice.getId().toString(), result.getFirst().getId());
        assertEquals(this.firstInvoice.getId().toString(), result.get(1).getId());
    }

    @Test
    void shouldFindByEngagementIdPrefixAndEmissionDateOrderByEmissionDateDesc() {
        String engagementPrefix = this.firstInvoice.getEngagement().getId().toString().substring(0, 4);
        List<InvoiceEntity> result = this.invoiceRepository
                .findByEngagementIdStartingWithAndEmissionDateGreaterThanEqualOrderByEmissionDateDesc(
                        engagementPrefix, LocalDate.of(2026, 3, 22));

        assertEquals(1, result.size());
        assertEquals(this.secondInvoice.getId().toString(), result.getFirst().getId());
    }

    @Test
    void shouldFindFirstBySeriesOrderByNumberDesc() {
        var optional = this.invoiceRepository.findFirstBySeriesOrderByNumberDesc("A");

        assertTrue(optional.isPresent());
        assertEquals(2, optional.get().getNumber());
    }
}
