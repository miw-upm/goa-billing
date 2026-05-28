package es.upm.api.configurations;

import es.upm.api.adapter.out.billing.mongo.expense.ExpenseEntity;
import es.upm.api.adapter.out.billing.mongo.expense.ExpenseRepository;
import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceEntity;
import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceRepository;
import es.upm.api.adapter.out.billing.mongo.payment.PaymentEntity;
import es.upm.api.adapter.out.billing.mongo.payment.PaymentRepository;
import es.upm.api.domain.model.BillingInfo;
import es.upm.api.domain.model.PaymentMethod;
import es.upm.api.domain.model.SupplierInfo;
import es.upm.api.domain.model.TaxCategory;
import es.upm.api.domain.model.creation.InvoiceLegalProcedure;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Log4j2
@Service
@Profile({"dev", "test"})
public class DatabaseSeederDev {

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
    private final ExpenseRepository expenseRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    public DatabaseSeederDev(ExpenseRepository expenseRepository, InvoiceRepository invoiceRepository, PaymentRepository paymentRepository) {
        this.expenseRepository = expenseRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
    }

    @PostConstruct
    public void deleteAllAndInitializeAndSeedDataBase() {
        this.deleteAllAndInitialize();
        this.seedDataBaseJava();
    }

    private void deleteAllAndInitialize() {
        this.invoiceRepository.deleteAll();
        this.expenseRepository.deleteAll();
        this.paymentRepository.deleteAll();
        log.warn("------- Delete All -----------");
    }

    private void seedDataBaseJava() {
        this.expenseRepository.saveAll(List.of(
                ExpenseEntity.builder()
                        .id(ID_10)
                        .recordedAt(LocalDateTime.of(2026, 3, 15, 9, 0))
                        .engagementId(EL_0)
                        .baseAmount(new BigDecimal("35.50"))
                        .vatRate(21)
                        .supplier(SupplierInfo.builder()
                                .name("Taxi Madrid")
                                .identity("A10000000")
                                .build())
                        .taxCategory(TaxCategory.OTROS)
                        .issueDate(LocalDate.of(2026, 3, 15))
                        .description("Taxi")
                        .withholdingTax(BigDecimal.ZERO)
                        .documentPath(null)
                        .build(),
                ExpenseEntity.builder()
                        .id(ID_11)
                        .recordedAt(LocalDateTime.of(2026, 3, 16, 9, 0))
                        .engagementId(EL_1)
                        .baseAmount(new BigDecimal("120.00"))
                        .vatRate(21)
                        .supplier(SupplierInfo.builder()
                                .name("Hotel Central")
                                .identity("B20000000")
                                .build())
                        .taxCategory(TaxCategory.SERVICIOS_PROFESIONALES)
                        .issueDate(LocalDate.of(2026, 3, 16))
                        .description("Hotel")
                        .withholdingTax(BigDecimal.ZERO)
                        .documentPath(null)
                        .build(),
                ExpenseEntity.builder()
                        .id(ID_12)
                        .recordedAt(LocalDateTime.of(2026, 3, 17, 9, 0))
                        .engagementId(null)
                        .baseAmount(new BigDecimal("18.90"))
                        .vatRate(21)
                        .supplier(SupplierInfo.builder()
                                .name("Restaurante Norte")
                                .identity("C30000000")
                                .build())
                        .taxCategory(TaxCategory.MANUTENCION)
                        .issueDate(LocalDate.of(2026, 3, 17))
                        .description("Comida")
                        .withholdingTax(BigDecimal.ZERO)
                        .documentPath(null)
                        .build(),
                ExpenseEntity.builder()
                        .id(ID_13)
                        .recordedAt(LocalDateTime.of(2026, 3, 18, 9, 0))
                        .engagementId(null)
                        .baseAmount(new BigDecimal("64.80"))
                        .vatRate(21)
                        .supplier(SupplierInfo.builder()
                                .name("Restaurante Sur")
                                .identity("D40000000")
                                .build())
                        .taxCategory(TaxCategory.MANUTENCION)
                        .issueDate(LocalDate.of(2026, 3, 18))
                        .description("Cena")
                        .withholdingTax(BigDecimal.ZERO)
                        .documentPath(null)
                        .build()
        ));
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
        this.invoiceRepository.saveAll(List.of(
                InvoiceEntity.builder()
                        .id(ID_14)
                        .concept("Cierre de hoja de encargo")
                        .closed(true)
                        .billingInfo(BillingInfo.builder()
                                .userId(C_0)
                                .fullName("User 0000")
                                .identity("ID-00000000A")
                                .fullAddress("Madrid, Spain")
                                .build())
                        .percentage(new BigDecimal("100"))
                        .legalProcedures(List.of(
                                InvoiceLegalProcedure.builder()
                                        .title("Procedimiento penal")
                                        .budget(new BigDecimal("450.00"))
                                        .legalTasks(List.of("Revision", "Escrito"))
                                        .build()
                        ))
                        .emissionDate(LocalDate.of(2026, 3, 20))
                        .operationDate(LocalDate.of(2026, 3, 20))
                        .series("2026")
                        .number(30)
                        .baseAmount(new BigDecimal("450.00"))
                        .vatRate(new BigDecimal("21"))
                        .vatAmount(new BigDecimal("94.50"))
                        .engagementId(null)
                        .payments(null)
                        .discounts(List.of(new BigDecimal("50.00")))
                        .pdfPath(null)
                        .rectification(null)
                        .build(),
                InvoiceEntity.builder()
                        .id(ID_15)
                        .concept("Provision de fondos")
                        .closed(false)
                        .billingInfo(BillingInfo.builder()
                                .userId(C_1)
                                .fullName("User 0001")
                                .identity("ID-00000001B")
                                .fullAddress("Madrid, Spain")
                                .build())
                        .percentage(new BigDecimal("100"))
                        .legalProcedures(List.of(
                                InvoiceLegalProcedure.builder()
                                        .title("Procedimiento civil")
                                        .budget(new BigDecimal("1200.00"))
                                        .legalTasks(List.of("Demanda"))
                                        .build()
                        ))
                        .baseAmount(new BigDecimal("1075.00"))
                        .vatRate(new BigDecimal("21"))
                        .vatAmount(new BigDecimal("225.75"))
                        .engagementId(null)
                        .payments(null)
                        .discounts(List.of(new BigDecimal("125.00")))
                        .pdfPath(null)
                        .rectification(null)
                        .build()
        ));
        log.warn("------- Initial Load from JAVA ---------------------------------------------------------------");
    }

}
