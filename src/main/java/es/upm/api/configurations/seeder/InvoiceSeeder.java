package es.upm.api.configurations.seeder;

import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceEntity;
import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceRepository;
import es.upm.api.domain.model.BillingInfo;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.Payment;
import es.upm.api.domain.model.PaymentMethod;
import es.upm.api.domain.model.external.EngagementSnapshot;
import es.upm.api.domain.model.external.UserSnapshot;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Log4j2
@Repository
@Profile({"dev", "test"})
public class InvoiceSeeder {

    private final InvoiceRepository invoiceRepository;

    public InvoiceSeeder(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public void seedDatabase() {
        log.warn("------- Invoice Initial Load -----------");
        List<InvoiceEntity> invoices = List.of(
                new InvoiceEntity(this.buildInvoice(
                        "cccccccc-dddd-eeee-ffff-aaaabbbb0001",
                        "aaaaaaa0-bbbb-cccc-dddd-eeeeffff0000",
                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0000",
                        LocalDate.of(2026, 3, 20),
                        LocalDate.of(2026, 3, 20),
                        List.of(
                                this.buildPayment("bbbbbbbb-cccc-dddd-eeee-ffffaaaab001",
                                        "aaaaaaa0-bbbb-cccc-dddd-eeeeffff0000",
                                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0000",
                                        "500.00", PaymentMethod.TRANSFER, LocalDate.of(2026, 3, 20))
                        ),
                        List.of(new BigDecimal("50.00"))
                )),
                new InvoiceEntity(this.buildInvoice(
                        "cccccccc-dddd-eeee-ffff-aaaabbbb0002",
                        "aaaaaaa0-bbbb-cccc-dddd-eeeeffff0001",
                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0001",
                        LocalDate.of(2026, 3, 21),
                        LocalDate.of(2026, 3, 21),
                        List.of(
                                this.buildPayment("bbbbbbbb-cccc-dddd-eeee-ffffaaaab002",
                                        "aaaaaaa0-bbbb-cccc-dddd-eeeeffff0001",
                                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0001",
                                        "1200.00", PaymentMethod.BIZUM, LocalDate.of(2026, 3, 21)),
                                this.buildPayment("bbbbbbbb-cccc-dddd-eeee-ffffaaaab003",
                                        "aaaaaaa0-bbbb-cccc-dddd-eeeeffff0001",
                                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0000",
                                        "200.00", PaymentMethod.CASH, LocalDate.of(2026, 3, 22))
                        ),
                        List.of(new BigDecimal("125.00"))
                ))
        );
        this.invoiceRepository.saveAll(invoices);
    }

    public void deleteAll() {
        this.invoiceRepository.deleteAll();
    }

    private Invoice buildInvoice(String id, String engagementId, String userId,
                                 LocalDate emissionDate, LocalDate operationDate,
                                 List<Payment> payments, List<BigDecimal> discounts) {
        BigDecimal paymentsTotal = payments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discountsTotal = discounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return Invoice.builder()
                .id(UUID.fromString(id))
                .billingInfo(BillingInfo.builder()
                        .userId(UUID.fromString(userId))
                        .fullName("User " + userId.substring(0, 4))
                        .identity("ID-" + userId.substring(0, 8))
                        .fullAddress("Madrid, Spain")
                        .build())
                .emissionDate(emissionDate)
                .operationDate(operationDate)
                .series("A")
                .number(1)
                .baseAmount(paymentsTotal.subtract(discountsTotal))
                .vatRate(new BigDecimal("21"))
                .engagement(EngagementSnapshot.builder().engagementId(UUID.fromString(engagementId)).build())
                .payments(payments)
                .discounts(discounts)
                .pdfPath(null)
                .rectification(null)
                .build();
    }

    private Payment buildPayment(String id, String engagementId, String userId,
                                 String amount, PaymentMethod method, LocalDate date) {
        return Payment.builder()
                .id(UUID.fromString(id))
                .engagement(EngagementSnapshot.builder().engagementId(UUID.fromString(engagementId)).build())
                .user(UserSnapshot.builder().id(UUID.fromString(userId)).build())
                .amount(new BigDecimal(amount))
                .method(method)
                .date(date)
                .build();
    }
}
