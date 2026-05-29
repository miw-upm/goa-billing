package es.upm.api.configurations;

import es.upm.api.adapter.out.billing.mongo.expense.ExpenseEntity;
import es.upm.api.adapter.out.billing.mongo.expense.SupplierInfoEntity;
import es.upm.api.adapter.out.billing.mongo.invoice.BillingInfoEntity;
import es.upm.api.adapter.out.billing.mongo.invoice.LegalProcedureEntity;
import es.upm.api.adapter.out.billing.mongo.expense.ExpenseRepository;
import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceEntity;
import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceRepository;
import es.upm.api.adapter.out.billing.mongo.payment.PaymentEntity;
import es.upm.api.adapter.out.billing.mongo.payment.PaymentRepository;
import es.upm.api.domain.model.PaymentMethod;
import es.upm.api.domain.model.TaxCategory;
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

    public static final String ID_0 = "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0000";
    public static final String ID_1 = "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0001";
    public static final String ID_2 = "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0002";
    public static final String ID_3 = "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0003";
    public static final String ID_4 = "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0004";
    public static final String ID_5 = "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0005";
    public static final String ID_6 = "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0006";
    public static final String ID_7 = "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0007";
    public static final String ID_8 = "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0008";
    public static final String ID_9 = "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0009";
    public static final String ID_10 = "aaaaaaaa-bbbb-cccc-dddd-eeeeffff000a";
    public static final String ID_11 = "aaaaaaaa-bbbb-cccc-dddd-eeeeffff000b";
    public static final String ID_12 = "aaaaaaaa-bbbb-cccc-dddd-eeeeffff000c";
    public static final String ID_13 = "aaaaaaaa-bbbb-cccc-dddd-eeeeffff000d";
    public static final String ID_14 = "aaaaaaaa-bbbb-cccc-dddd-eeeeffff000e";
    public static final String ID_15 = "aaaaaaaa-bbbb-cccc-dddd-eeeeffff000f";
    public static final String ID_16 = "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0010";
    public static final String ID_17 = "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0011";
    public static final UUID EL_0 = UUID.fromString(ID_0);
    public static final UUID EL_1 = UUID.fromString(ID_1);
    public static final UUID C_0 = UUID.fromString(ID_0);
    public static final UUID C_1 = UUID.fromString(ID_1);
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
                        .engagementId(EL_0.toString())
                        .baseAmount(new BigDecimal("35.50"))
                        .vatRate(21)
                        .supplier(new SupplierInfoEntity("Taxi Madrid", "A10000000"))
                        .taxCategory(TaxCategory.OTROS)
                        .issueDate(LocalDate.of(2026, 3, 15))
                        .description("Taxi")
                        .withholdingTax(BigDecimal.ZERO)
                        .documentPath(null)
                        .build(),
                ExpenseEntity.builder()
                        .id(ID_11)
                        .recordedAt(LocalDateTime.of(2026, 3, 16, 9, 0))
                        .engagementId(EL_1.toString())
                        .baseAmount(new BigDecimal("120.00"))
                        .vatRate(21)
                        .supplier(new SupplierInfoEntity("Hotel Central", "B20000000"))
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
                        .supplier(new SupplierInfoEntity("Restaurante Norte", "C30000000"))
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
                        .supplier(new SupplierInfoEntity("Restaurante Sur", "D40000000"))
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
                        .engagementId(EL_0.toString())
                        .userId(C_0.toString())
                        .amount(new BigDecimal("500.00"))
                        .method(PaymentMethod.TRANSFER)
                        .date(LocalDate.of(2026, 3, 20))
                        .invoiced(false)
                        .build(),
                PaymentEntity.builder()
                        .id(ID_1)
                        .engagementId(EL_1.toString())
                        .userId(C_1.toString())
                        .amount(new BigDecimal("1200.00"))
                        .method(PaymentMethod.BIZUM)
                        .date(LocalDate.of(2026, 3, 21))
                        .invoiced(false)
                        .build(),
                PaymentEntity.builder()
                        .id(ID_2)
                        .engagementId(EL_1.toString())
                        .userId(C_0.toString())
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
                        .billingInfo(new BillingInfoEntity(
                                C_0.toString(),
                                "User 0000",
                                "ID-00000000A",
                                "Madrid, Spain"
                        ))
                        .percentage(new BigDecimal("100"))
                        .legalProcedures(List.of(
                                new LegalProcedureEntity(
                                        "Procedimiento penal",
                                        new BigDecimal("450.00"),
                                        List.of("Revision", "Escrito")
                                )
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
                        .billingInfo(new BillingInfoEntity(
                                C_1.toString(),
                                "User 0001",
                                "ID-00000001B",
                                "Madrid, Spain"
                        ))
                        .percentage(new BigDecimal("100"))
                        .legalProcedures(List.of(
                                new LegalProcedureEntity(
                                        "Procedimiento civil",
                                        new BigDecimal("1200.00"),
                                        List.of("Demanda")
                                )
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
