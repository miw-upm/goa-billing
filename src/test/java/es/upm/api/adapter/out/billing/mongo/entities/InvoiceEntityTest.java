package es.upm.api.adapter.out.billing.mongo.entities;

import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceEntity;
import es.upm.api.adapter.out.billing.mongo.invoice.InvoicedExpenseEntity;
import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceLegalProcedureEntity;
import es.upm.api.adapter.out.billing.mongo.invoice.InvoicedPaymentEntity;
import es.upm.api.domain.model.*;
import es.upm.api.domain.model.creation.InvoiceLegalProcedure;
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
                .concept("Servicios")
                .closed(true)
                .billingInfo(BillingInfo.builder()
                        .userId(userId)
                        .fullName("John Doe")
                        .identity("12345678A")
                        .fullAddress("Madrid")
                        .build())
                .percentage(new BigDecimal("100"))
                .emissionDate(LocalDate.of(2026, 3, 20))
                .operationDate(LocalDate.of(2026, 3, 19))
                .series("A")
                .number(1)
                .baseAmount(BigDecimal.valueOf(90))
                .vatAmount(BigDecimal.valueOf(18.9))
                .vatRate(BigDecimal.valueOf(21))
                .engagement(EngagementSnapshot.builder().id(engagementId).build())
                .legalProcedures(List.of(InvoiceLegalProcedure.builder()
                        .title("Procedimiento")
                        .budget(new BigDecimal("90.00"))
                        .legalTasks(List.of("Tarea 1"))
                        .build()))
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
                        BigDecimal.valueOf(50),
                        PaymentMethod.CASH,
                        UserSnapshot.builder().id(userId).build()
                )))
                .expenses(List.of(new InvoicedExpense(
                        UUID.randomUUID(),
                        LocalDate.of(2026, 3, 11),
                        "gasto",
                        BigDecimal.valueOf(30),
                        BigDecimal.valueOf(6.3)
                )))
                .discounts(List.of(BigDecimal.TEN))
                .pdfPath("/tmp/invoice.pdf")
                .build();
    }

    @Test
    void shouldBuildInvoiceEntityFromInvoice() {
        InvoiceEntity entity = new InvoiceEntity(this.invoice);

        assertEquals(this.invoice.getId().toString(), entity.getId());
        assertEquals(this.invoice.getConcept(), entity.getConcept());
        assertEquals(this.invoice.getClosed(), entity.getClosed());
        assertEquals(this.invoice.getBillingInfo(), entity.getBillingInfo());
        assertEquals(this.invoice.getPercentage(), entity.getPercentage());
        assertEquals(this.invoice.getEmissionDate(), entity.getEmissionDate());
        assertEquals(this.invoice.getOperationDate(), entity.getOperationDate());
        assertEquals(this.invoice.getSeries(), entity.getSeries());
        assertEquals(this.invoice.getNumber(), entity.getNumber());
        assertEquals(this.invoice.getBaseAmount(), entity.getBaseAmount());
        assertEquals(this.invoice.getVatAmount(), entity.getVatAmount());
        assertEquals(this.invoice.getVatRate(), entity.getVatRate());
        assertEquals(this.invoice.getEngagement().getId().toString(), entity.getEngagementId());
        assertEquals(this.invoice.getLegalProcedures(),
                entity.getLegalProcedures().stream().map(InvoiceLegalProcedureEntity::toDomain).toList());
        assertEquals(this.invoice.getPayments(), entity.getPayments().stream().map(InvoicedPaymentEntity::toDomain).toList());
        assertEquals(this.invoice.getPriorPayments(), entity.getPriorPayments().stream().map(InvoicedPaymentEntity::toDomain).toList());
        assertEquals(this.invoice.getExpenses(), entity.getExpenses().stream().map(InvoicedExpenseEntity::toDomain).toList());
        assertEquals(this.invoice.getDiscounts(), entity.getDiscounts());
        assertEquals(this.invoice.getPdfPath(), entity.getPdfPath());
    }

    @Test
    void shouldConvertInvoiceEntityToDomain() {
        InvoiceEntity entity = new InvoiceEntity(this.invoice);

        Invoice mapped = entity.toDomain();

        assertEquals(UUID.fromString(entity.getId()), mapped.getId());
        assertEquals(entity.getConcept(), mapped.getConcept());
        assertEquals(entity.getClosed(), mapped.getClosed());
        assertEquals(entity.getBillingInfo(), mapped.getBillingInfo());
        assertEquals(entity.getPercentage(), mapped.getPercentage());
        assertEquals(entity.getEmissionDate(), mapped.getEmissionDate());
        assertEquals(entity.getOperationDate(), mapped.getOperationDate());
        assertEquals(entity.getSeries(), mapped.getSeries());
        assertEquals(entity.getNumber(), mapped.getNumber());
        assertEquals(entity.getBaseAmount(), mapped.getBaseAmount());
        assertEquals(entity.getVatAmount(), mapped.getVatAmount());
        assertEquals(entity.getVatRate(), mapped.getVatRate());
        assertEquals(UUID.fromString(entity.getEngagementId()), mapped.getEngagement().getId());
        assertEquals(this.invoice.getLegalProcedures(), mapped.getLegalProcedures());
        assertEquals(this.invoice.getPayments(), mapped.getPayments());
        assertEquals(this.invoice.getPriorPayments(), mapped.getPriorPayments());
        assertEquals(this.invoice.getExpenses(), mapped.getExpenses());
        assertEquals(entity.getDiscounts(), mapped.getDiscounts());
        assertEquals(entity.getPdfPath(), mapped.getPdfPath());
    }
}
