package es.upm.api.configurations;

import es.upm.api.adapter.out.billing.mongo.payment.PaymentEntity;
import es.upm.api.adapter.out.billing.mongo.payment.PaymentRepository;
import es.upm.api.configurations.seeder.ExpenseSeeder;
import es.upm.api.configurations.seeder.InvoiceSeeder;
import es.upm.api.domain.model.PaymentMethod;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Log4j2
@Service
@Profile({"dev", "test"})
public class DatabaseSeederDev {

    private final ExpenseSeeder expenseSeeder;
    private final InvoiceSeeder invoiceSeeder;
    private final PaymentRepository paymentRepository;

    public DatabaseSeederDev(ExpenseSeeder expenseSeeder, InvoiceSeeder invoiceSeeder, PaymentRepository paymentRepository) {
        this.expenseSeeder = expenseSeeder;
        this.invoiceSeeder = invoiceSeeder;
        this.paymentRepository = paymentRepository;
    }

    public static final UUID ID_0 = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeffff0000");
    public static final UUID ID_1 = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeffff0001");
    public static final UUID ID_2 = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeffff0002");
    public static final UUID ID_3 = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeffff0003");
    public static final UUID ID_4 = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeffff0004");
    public static final UUID ID_5 = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeffff0005");
    public static final UUID ID_6 = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeffff0006");
    public static final UUID ID_7 = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeffff0007");
    public static final UUID ID_8 = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeffff0008");
    public static final UUID ID_9 = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeffff0009");
    public static final UUID ID_10 = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeffff000a");
    public static final UUID ID_11 = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeffff000b");
    public static final UUID ID_12 = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeffff000c");
    public static final UUID ID_13 = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeffff000d");
    public static final UUID ID_14 = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeffff000e");
    public static final UUID ID_15 = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeffff000f");
    public static final UUID ID_16 = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeffff0010");
    public static final UUID ID_17 = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeffff0011");

    public static final UUID EL_0 = ID_0;
    public static final UUID EL_1 = ID_1;

    public static final UUID C_0 = ID_0;
    public static final UUID C_1 = ID_1;

    @PostConstruct
    public void deleteAllAndInitializeAndSeedDataBase() {
        this.deleteAllAndInitialize();
        this.seedDataBaseJava();
    }

    private void deleteAllAndInitialize() {
        this.invoiceSeeder.deleteAll();
        this.expenseSeeder.deleteAll();
        this.paymentRepository.deleteAll();
        log.warn("------- Delete All -----------");
    }

    private void seedDataBaseJava() {
        this.expenseSeeder.seedDatabase();
        this.paymentRepository.saveAll(List.of(
                PaymentEntity.builder()
                        .id(ID_0)
                        .engagementId(EL_0)
                        .userId(C_0)
                        .amount(new BigDecimal("500.00"))
                        .method(PaymentMethod.TRANSFER)
                        .date(LocalDate.of(2026, 3, 20))
                        .invoiced(false)
                        .build(),
                PaymentEntity.builder()
                        .id(ID_1)
                        .engagementId(EL_1)
                        .userId(C_1)
                        .amount(new BigDecimal("1200.00"))
                        .method(PaymentMethod.BIZUM)
                        .date(LocalDate.of(2026, 3, 21))
                        .invoiced(false)
                        .build(),
                PaymentEntity.builder()
                        .id(ID_2)
                        .engagementId(EL_1)
                        .userId(C_0)
                        .amount(new BigDecimal("200.00"))
                        .method(PaymentMethod.CASH)
                        .date(LocalDate.of(2026, 3, 22))
                        .invoiced(true)
                        .build()
        ));
        this.invoiceSeeder.seedDatabase();
        log.warn("------- Initial Load from JAVA ---------------------------------------------------------------");
    }

}
