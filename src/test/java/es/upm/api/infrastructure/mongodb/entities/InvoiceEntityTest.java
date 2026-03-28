package es.upm.api.infrastructure.mongodb.entities;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.Income;
import es.upm.api.domain.model.Invoice;
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
        this.invoice = Invoice.builder()
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
        InvoiceEntity invoiceEntity = new InvoiceEntity(this.invoice);

        assertEquals(this.invoice.getId(), invoiceEntity.getId());
        assertEquals(this.invoice.getEngagementId(), invoiceEntity.getEngagementId());
        assertEquals(this.invoice.getDate(), invoiceEntity.getDate());
        assertEquals(this.invoice.getExpenses(), invoiceEntity.getExpenses());
        assertEquals(this.invoice.getIncomes(), invoiceEntity.getIncomes());
    }

    @Test
    void shouldConvertInvoiceEntityToInvoice() {
        InvoiceEntity invoiceEntity = new InvoiceEntity();
        invoiceEntity.setId(this.invoice.getId());
        invoiceEntity.setEngagementId(this.invoice.getEngagementId());
        invoiceEntity.setDate(this.invoice.getDate());
        invoiceEntity.setExpenses(this.invoice.getExpenses());
        invoiceEntity.setIncomes(this.invoice.getIncomes());

        Invoice mappedInvoice = invoiceEntity.toInvoice();

        assertEquals(invoiceEntity.getId(), mappedInvoice.getId());
        assertEquals(invoiceEntity.getEngagementId(), mappedInvoice.getEngagementId());
        assertEquals(invoiceEntity.getDate(), mappedInvoice.getDate());
        assertEquals(invoiceEntity.getExpenses(), mappedInvoice.getExpenses());
        assertEquals(invoiceEntity.getIncomes(), mappedInvoice.getIncomes());
    }
}
