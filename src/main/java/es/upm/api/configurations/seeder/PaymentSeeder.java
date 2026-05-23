package es.upm.api.configurations.seeder;

import es.upm.api.adapter.out.billing.mongo.payment.PaymentEntity;
import es.upm.api.adapter.out.billing.mongo.payment.PaymentRepository;
import es.upm.api.domain.model.PaymentMethod;
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
public class PaymentSeeder {

    private final PaymentRepository paymentRepository;

    public PaymentSeeder(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public void seedDatabase() {
        log.warn("------- Payment Initial Load -----------");
        List<PaymentEntity> payments = List.of(
                this.buildPayment(
                        "bbbbbbbb-cccc-dddd-eeee-ffffaaaab001",
                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0000",
                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0000",
                        "500.00",
                        PaymentMethod.TRANSFER,
                        LocalDate.of(2026, 3, 20),
                        false
                ),
                this.buildPayment(
                        "bbbbbbbb-cccc-dddd-eeee-ffffaaaab002",
                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0001",
                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0001",
                        "1200.00",
                        PaymentMethod.BIZUM,
                        LocalDate.of(2026, 3, 21),
                        false
                ),
                this.buildPayment(
                        "bbbbbbbb-cccc-dddd-eeee-ffffaaaab003",
                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0001",
                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0000",
                        "200.00",
                        PaymentMethod.CASH,
                        LocalDate.of(2026, 3, 22),
                        true
                )
        );
        this.paymentRepository.saveAll(payments);
    }

    public void deleteAll() {
        this.paymentRepository.deleteAll();
    }

    private PaymentEntity buildPayment(String id, String engagementId, String userId, String amount,
                                       PaymentMethod method, LocalDate date, boolean invoiced) {
        return PaymentEntity.builder()
                .id(UUID.fromString(id))
                .engagementId(UUID.fromString(engagementId))
                .userId(UUID.fromString(userId))
                .amount(new BigDecimal(amount))
                .method(method)
                .date(date)
                .invoiced(invoiced)
                .build();
    }
}
