package es.upm.api.adapter.out.billing.mongo.entities;

import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceEntity;
import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.Income;
import es.upm.api.domain.model.InvoiceOld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvoiceOldEntityTest {

    private InvoiceOld invoiceOld;

    @BeforeEach
    void setUp() {
        UUID engagementId = UUID.randomUUID();
        this.invoiceOld = InvoiceOld.builder()
                .id(UUID.randomUUID())
                .engagementId(engagementId)
                .date(LocalDate.of(2026, 3, 21))
                .expenses(List.of(Expense.builder()
                        .id(UUID.randomUUID())
                        .engagementId(engagementId)
                        .amount(BigDecimal.valueOf(25))
                        .date(LocalDate.of(2026, 3, 20))
                        .description("Taxi")
                        .build()))
                .incomes(List.of(Income.builder()
                        .id(UUID.randomUUID())
                        .engagementId(engagementId)
                        .userId(UUID.randomUUID())
                        .amount(BigDecimal.valueOf(250))
                        .date(LocalDate.of(2026, 3, 20))
                        .build()))
                .build();
    }

    @Test
    void shouldBuildInvoiceEntityFromInvoice() {
        InvoiceEntity invoiceEntity = new InvoiceEntity(this.invoiceOld);

        assertEquals(this.invoiceOld.getId(), invoiceEntity.getId());
        assertEquals(this.invoiceOld.getEngagementId(), invoiceEntity.getEngagementId());
        assertEquals(this.invoiceOld.getDate(), invoiceEntity.getDate());
        assertEquals(this.invoiceOld.getExpenses(), invoiceEntity.getExpenses());
        assertEquals(this.invoiceOld.getIncomes(), invoiceEntity.getIncomes());
    }

    @Test
    void shouldConvertInvoiceEntityToDomain() {
        InvoiceEntity invoiceEntity = new InvoiceEntity();
        invoiceEntity.setId(this.invoiceOld.getId());
        invoiceEntity.setEngagementId(this.invoiceOld.getEngagementId());
        invoiceEntity.setDate(this.invoiceOld.getDate());
        invoiceEntity.setExpenses(this.invoiceOld.getExpenses());
        invoiceEntity.setIncomes(this.invoiceOld.getIncomes());

        InvoiceOld mappedInvoiceOld = invoiceEntity.toDomain();

        assertEquals(invoiceEntity.getId(), mappedInvoiceOld.getId());
        assertEquals(invoiceEntity.getEngagementId(), mappedInvoiceOld.getEngagementId());
        assertEquals(invoiceEntity.getDate(), mappedInvoiceOld.getDate());
        assertEquals(invoiceEntity.getExpenses(), mappedInvoiceOld.getExpenses());
        assertEquals(invoiceEntity.getIncomes(), mappedInvoiceOld.getIncomes());
    }
}
