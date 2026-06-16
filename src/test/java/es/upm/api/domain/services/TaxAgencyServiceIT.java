package es.upm.api.domain.services;

import es.upm.api.domain.model.BillingInfo;
import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.SupplierInfo;
import es.upm.api.domain.model.TaxCategory;
import es.upm.api.domain.model.report.VatSummary;
import es.upm.api.domain.ports.out.billing.ExpenseGateway;
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
    private static final BigDecimal INVESTMENT_ASSET_THRESHOLD = new BigDecimal("3005.06");

    @Autowired
    private TaxAgencyService taxAgencyService;
    @MockitoBean
    private InvoiceGateway invoiceGateway;
    @MockitoBean
    private ExpenseGateway expenseGateway;

    @Test
    void shouldFindInvoiceIssuedBookByDateRange() {
        LocalDate fromDate = LocalDate.of(2026, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 3, 31);
        Invoice first = this.buildInvoice(31, LocalDate.of(2026, 1, 20), LocalDate.of(2026, 1, 19),
                "12345678Z", "First Client", "100.00", "21.00");
        Invoice second = this.buildInvoice(32, LocalDate.of(2026, 2, 20), LocalDate.of(2026, 2, 19),
                "87654321X", "Second Client", "200.00", "42.00");
        when(this.invoiceGateway.findIssuedBetween(fromDate, toDate)).thenReturn(Stream.of(first, second));

        List<Invoice> invoices = this.taxAgencyService.invoiceIssuedBook(fromDate, toDate);

        assertEquals(List.of(first, second), invoices);
        verify(this.invoiceGateway).findIssuedBetween(fromDate, toDate);
    }

    @Test
    void shouldFindReceivedBookByDateRange() {
        LocalDate fromDate = LocalDate.of(2026, 4, 1);
        LocalDate toDate = LocalDate.of(2026, 6, 30);
        Expense first = this.buildExpense(LocalDate.of(2026, 4, 10));
        Expense second = this.buildExpense(LocalDate.of(2026, 5, 15));
        when(this.expenseGateway.findInvoiceReceivedBook(fromDate, toDate, INVESTMENT_ASSET_THRESHOLD))
                .thenReturn(Stream.of(first, second));

        List<Expense> expenses = this.taxAgencyService
                .findInvoiceReceivedBook(fromDate, toDate, INVESTMENT_ASSET_THRESHOLD);

        assertEquals(List.of(first, second), expenses);
        verify(this.expenseGateway).findInvoiceReceivedBook(fromDate, toDate, INVESTMENT_ASSET_THRESHOLD);
    }

    @Test
    void shouldCountReceivedBookBeforeDate() {
        LocalDate fromDate = LocalDate.of(2026, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 3, 31);
        when(this.expenseGateway.countInvoiceReceivedBook(fromDate, toDate, INVESTMENT_ASSET_THRESHOLD)).thenReturn(3L);

        assertEquals(3, this.taxAgencyService.countInvoiceReceivedBook(fromDate, toDate, INVESTMENT_ASSET_THRESHOLD));
        verify(this.expenseGateway).countInvoiceReceivedBook(fromDate, toDate, INVESTMENT_ASSET_THRESHOLD);
    }

    @Test
    void shouldReturnEmptyListWhenThereAreNoInvoices() {
        LocalDate fromDate = LocalDate.of(2026, 10, 1);
        LocalDate toDate = LocalDate.of(2026, 12, 31);
        when(this.invoiceGateway.findIssuedBetween(fromDate, toDate)).thenReturn(Stream.empty());

        assertEquals(List.of(), this.taxAgencyService.invoiceIssuedBook(fromDate, toDate));
        verify(this.invoiceGateway).findIssuedBetween(fromDate, toDate);
    }

    @Test
    void shouldBuildModel303() {
        LocalDate fromDate = LocalDate.of(2026, 4, 1);
        LocalDate toDate = LocalDate.of(2026, 6, 30);
        Invoice firstInvoice = this.buildInvoice(31, LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 19),
                "12345678Z", "First Client", "100.00", "21.00");
        Invoice secondInvoice = this.buildInvoice(32, LocalDate.of(2026, 5, 20), LocalDate.of(2026, 5, 19),
                "87654321X", "Second Client", "200.00", "8.00");
        Expense currentExpense = this.buildExpense(LocalDate.of(2026, 4, 10), "50.00", 21, 100);
        Expense reducedVatCurrentExpense = this.buildExpense(LocalDate.of(2026, 5, 10), "25.00", 4, 100);
        Expense investmentExpense = this.buildExpense(LocalDate.of(2026, 6, 10), "4000.00", 21, 10);
        when(this.invoiceGateway.findIssuedBetween(fromDate, toDate))
                .thenReturn(Stream.of(firstInvoice, secondInvoice));
        when(this.expenseGateway.findInvoiceReceivedBook(fromDate, toDate, INVESTMENT_ASSET_THRESHOLD))
                .thenReturn(Stream.of(currentExpense, reducedVatCurrentExpense));
        when(this.expenseGateway.findInvoiceReceivedInvestmentBook(fromDate, toDate, INVESTMENT_ASSET_THRESHOLD))
                .thenReturn(Stream.of(investmentExpense));

        VatSummary result = this.taxAgencyService.vatSummary(fromDate, toDate);

        assertEquals(new VatSummary(
                new BigDecimal("300.00"),
                new BigDecimal("29.00"),
                new BigDecimal("75.00"),
                new BigDecimal("11.50"),
                new BigDecimal("4000.00"),
                new BigDecimal("840.00")
        ), result);
        verify(this.invoiceGateway).findIssuedBetween(fromDate, toDate);
        verify(this.expenseGateway).findInvoiceReceivedBook(fromDate, toDate, INVESTMENT_ASSET_THRESHOLD);
        verify(this.expenseGateway).findInvoiceReceivedInvestmentBook(fromDate, toDate, INVESTMENT_ASSET_THRESHOLD);
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

    private Expense buildExpense(LocalDate issueDate) {
        return this.buildExpense(issueDate, "100.00", 21, 100);
    }

    private Expense buildExpense(LocalDate issueDate, String baseAmount, int vatRate, int depreciationRate) {
        return Expense.builder()
                .id(UUID.randomUUID())
                .issueDate(issueDate)
                .baseAmount(new BigDecimal(baseAmount))
                .vatRate(vatRate)
                .supplier(SupplierInfo.builder()
                        .name("Supplier")
                        .identity("B10000000")
                        .build())
                .taxCategory(TaxCategory.OTROS)
                .depreciationRate(depreciationRate)
                .build();
    }
}
