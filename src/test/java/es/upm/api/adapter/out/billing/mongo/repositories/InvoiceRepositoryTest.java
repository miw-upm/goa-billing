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
                        .depreciationRate(100)
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
    void shouldFindByEmissionDateGreaterThanEqualOrderBySeriesDescNumberDesc() {
        Invoice higherSeries = this.buildMinimalInvoice(1, LocalDate.of(2026, 3, 22), LocalDate.of(2026, 3, 22));
        higherSeries.setSeries("B");
        this.invoiceRepository.save(new InvoiceEntity(higherSeries));

        List<InvoiceEntity> byDate = this.invoiceRepository.findByEmissionDateGreaterThanEqualOrderBySeriesDescNumberDesc(
                LocalDate.of(2026, 3, 21)
        );

        assertEquals(2, byDate.size());
        assertEquals(higherSeries.getId().toString(), byDate.getFirst().getId());
        assertEquals(this.secondInvoice.getId().toString(), byDate.get(1).getId());
    }

    @Test
    void shouldFindAllOrderBySeriesDescNumberDesc() {
        Invoice higherSeries = this.buildMinimalInvoice(1, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 1));
        higherSeries.setSeries("B");
        Invoice higherNumber = this.buildMinimalInvoice(3, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 1));
        this.invoiceRepository.saveAll(List.of(new InvoiceEntity(higherSeries), new InvoiceEntity(higherNumber)));

        List<InvoiceEntity> result = this.invoiceRepository.findAllByOrderBySeriesDescNumberDesc();

        assertEquals(4, result.size());
        assertEquals(higherSeries.getId().toString(), result.getFirst().getId());
        assertEquals(higherNumber.getId().toString(), result.get(1).getId());
        assertEquals(this.secondInvoice.getId().toString(), result.get(2).getId());
        assertEquals(this.firstInvoice.getId().toString(), result.get(3).getId());
    }

    @Test
    void shouldFindByEngagementIdPrefixOrderBySeriesDescNumberDesc() {
        Invoice higherSeries = this.buildMinimalInvoice(1, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 1));
        higherSeries.setSeries("B");
        higherSeries.setEngagement(this.firstInvoice.getEngagement());
        this.invoiceRepository.save(new InvoiceEntity(higherSeries));

        String engagementPrefix = this.firstInvoice.getEngagement().getId().toString().substring(0, 4);
        List<InvoiceEntity> result = this.invoiceRepository.findByEngagementIdStartingWithOrderBySeriesDescNumberDesc(
                engagementPrefix);

        assertEquals(3, result.size());
        assertEquals(higherSeries.getId().toString(), result.getFirst().getId());
        assertEquals(this.secondInvoice.getId().toString(), result.get(1).getId());
        assertEquals(this.firstInvoice.getId().toString(), result.get(2).getId());
    }

    @Test
    void shouldFindByEngagementIdPrefixAndEmissionDateOrderBySeriesDescNumberDesc() {
        Invoice higherSeries = this.buildMinimalInvoice(1, LocalDate.of(2026, 3, 23), LocalDate.of(2026, 3, 23));
        higherSeries.setSeries("B");
        higherSeries.setEngagement(this.firstInvoice.getEngagement());
        this.invoiceRepository.save(new InvoiceEntity(higherSeries));

        String engagementPrefix = this.firstInvoice.getEngagement().getId().toString().substring(0, 4);
        List<InvoiceEntity> result = this.invoiceRepository
                .findByEngagementIdStartingWithAndEmissionDateGreaterThanEqualOrderBySeriesDescNumberDesc(
                        engagementPrefix, LocalDate.of(2026, 3, 22));

        assertEquals(2, result.size());
        assertEquals(higherSeries.getId().toString(), result.getFirst().getId());
        assertEquals(this.secondInvoice.getId().toString(), result.get(1).getId());
    }

    @Test
    void shouldFindIssuedBetweenUsingOperationDateOrEmissionDateFallbackOrderByNumberAsc() {
        Invoice fallbackEmissionDate = this.buildMinimalInvoice(4, LocalDate.of(2026, 3, 10), null);
        Invoice operationDateOutsideRange = this.buildMinimalInvoice(3, LocalDate.of(2026, 3, 10), LocalDate.of(2026, 4, 1));
        Invoice draft = this.buildMinimalInvoice(0, null, LocalDate.of(2026, 3, 10));
        this.invoiceRepository.saveAll(List.of(
                new InvoiceEntity(fallbackEmissionDate),
                new InvoiceEntity(operationDateOutsideRange),
                new InvoiceEntity(draft)
        ));

        List<InvoiceEntity> result = this.invoiceRepository.findIssuedBetweenOrderByNumberAsc(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertEquals(3, result.size());
        assertEquals(1, result.get(0).getNumber());
        assertEquals(2, result.get(1).getNumber());
        assertEquals(4, result.get(2).getNumber());
    }

    @Test
    void shouldFindIssuedBetweenNumbersOrderByNumberAsc() {
        Invoice draft = this.buildMinimalInvoice(3, null, LocalDate.of(2026, 3, 10));
        Invoice otherSeries = this.buildMinimalInvoice(1, LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 10));
        otherSeries.setSeries("B");
        this.invoiceRepository.saveAll(List.of(new InvoiceEntity(draft), new InvoiceEntity(otherSeries)));

        List<InvoiceEntity> result = this.invoiceRepository.findIssuedBetweenOrderByNumberAsc("A", 1, 3);

        assertEquals(2, result.size());
        assertEquals(1, result.getFirst().getNumber());
        assertEquals(2, result.get(1).getNumber());
    }

    @Test
    void shouldFindFirstBySeriesOrderByNumberDesc() {
        var optional = this.invoiceRepository.findFirstBySeriesOrderByNumberDesc("A");

        assertTrue(optional.isPresent());
        assertEquals(2, optional.get().getNumber());
    }

    @Test
    void shouldFindBySeriesAndNumber() {
        Optional<InvoiceEntity> optional = this.invoiceRepository.findBySeriesAndNumber("A", 1);

        assertTrue(optional.isPresent());
        assertEquals(this.firstInvoice.getId().toString(), optional.get().getId());
    }

    private Invoice buildMinimalInvoice(int number, LocalDate emissionDate, LocalDate operationDate) {
        return Invoice.builder()
                .id(UUID.randomUUID())
                .billingInfo(BillingInfo.builder()
                        .userId(UUID.randomUUID())
                        .fullName("Jane Doe")
                        .identity("87654321B")
                        .fullAddress("Madrid")
                        .build())
                .emissionDate(emissionDate)
                .operationDate(operationDate)
                .series("A")
                .number(number)
                .baseAmount(BigDecimal.TEN)
                .vatAmount(BigDecimal.ONE)
                .vatRate(BigDecimal.valueOf(21))
                .build();
    }
}
