package es.upm.api.domain.services;

import es.upm.api.domain.model.BillingInfo;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.ports.out.billing.InvoiceGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class TaxAgencyServiceIT {

    @Autowired
    private TaxAgencyService taxAgencyService;
    @MockitoBean
    private InvoiceGateway invoiceGateway;

    @Test
    void shouldFindInvoiceIssuedBookByDateRange() {
        LocalDate fromDate = LocalDate.of(2026, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 3, 31);
        Invoice first = this.buildInvoice(31, LocalDate.of(2026, 1, 20), LocalDate.of(2026, 1, 19),
                "11111111A", "First Client", "100.00", "21.00");
        Invoice second = this.buildInvoice(32, LocalDate.of(2026, 2, 20), LocalDate.of(2026, 2, 19),
                "22222222B", "Second Client", "200.00", "42.00");
        when(this.invoiceGateway.findIssuedBetween(fromDate, toDate)).thenReturn(Stream.of(first, second));

        List<Invoice> invoices = this.taxAgencyService.invoiceIssuedBook(fromDate, toDate);

        assertEquals(List.of(first, second), invoices);
        verify(this.invoiceGateway).findIssuedBetween(fromDate, toDate);
    }

    @Test
    void shouldReturnEmptyListWhenThereAreNoInvoices() {
        LocalDate fromDate = LocalDate.of(2026, 10, 1);
        LocalDate toDate = LocalDate.of(2026, 12, 31);
        when(this.invoiceGateway.findIssuedBetween(fromDate, toDate)).thenReturn(Stream.empty());

        assertEquals(List.of(), this.taxAgencyService.invoiceIssuedBook(fromDate, toDate));
        verify(this.invoiceGateway).findIssuedBetween(fromDate, toDate);
    }

    private Invoice buildInvoice(int number, LocalDate emissionDate, LocalDate operationDate,
                                 String identity, String fullName, String baseAmount, String vatAmount) {
        return Invoice.builder()
                .id(UUID.randomUUID())
                .series("2026")
                .number(number)
                .emissionDate(emissionDate)
                .operationDate(operationDate)
                .billingInfo(BillingInfo.builder()
                        .userId(UUID.randomUUID())
                        .identity(identity)
                        .fullName(fullName)
                        .fullAddress("Madrid")
                        .build())
                .baseAmount(new BigDecimal(baseAmount))
                .vatRate(new BigDecimal("21"))
                .vatAmount(new BigDecimal(vatAmount))
                .build();
    }
}
