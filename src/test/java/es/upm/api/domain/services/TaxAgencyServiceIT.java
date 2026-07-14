package es.upm.api.domain.services;

import es.upm.api.domain.model.*;
import es.upm.api.domain.model.report.InvoiceBookReport;
import es.upm.api.domain.model.report.NetIncomeBreakdownReport;
import es.upm.api.domain.model.report.Quarter;
import es.upm.api.domain.model.report.VatSummaryReport;
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
    void shouldFindInvoiceIssuedBookByQuarter() {
        LocalDate fromDate = LocalDate.of(2026, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 3, 31);
        Invoice first = this.buildInvoice(31, LocalDate.of(2026, 1, 20), LocalDate.of(2026, 1, 19),
                "12345678Z", "First Client", "100.00", "21.00");
        Invoice second = this.buildInvoice(32, LocalDate.of(2026, 2, 20), LocalDate.of(2026, 2, 19),
                "87654321X", "Second Client", "200.00", "42.00");
        when(this.invoiceGateway.findIssuedBetween(fromDate, toDate)).thenReturn(Stream.of(first, second));

        List<InvoiceBookReport> invoices = this.taxAgencyService.invoiceIssuedBook(2026, Quarter.T1);

        assertEquals(List.of(InvoiceBookReport.from(first), InvoiceBookReport.from(second)), invoices);
        verify(this.invoiceGateway).findIssuedBetween(fromDate, toDate);
    }

    @Test
    void shouldAddClosedInvoiceExpensesGroupedByVatRateToInvoiceIssuedBookReport() {
        Invoice invoice = this.buildInvoice(31, LocalDate.of(2026, 1, 20), LocalDate.of(2026, 1, 19),
                "12345678Z", "First Client", "100.00", "21.00");
        invoice.setClosed(true);
        invoice.setPercentage(new BigDecimal("50"));
        invoice.setExpenses(List.of(
                this.buildExpense(LocalDate.of(2026, 1, 10), "30.00", 21, 100),
                this.buildExpense(LocalDate.of(2026, 1, 11), "10.00", 21, 100),
                this.buildExpense(LocalDate.of(2026, 1, 12), "40.00", 10, 100)
        ));

        InvoiceBookReport report = InvoiceBookReport.from(invoice);

        assertEquals(List.of(21, 10), List.copyOf(report.vatLines().keySet()));
        this.assertBigDecimalEquals(new BigDecimal("120.00"), report.vatLines().get(21).baseAmount());
        this.assertBigDecimalEquals(new BigDecimal("25.20"), report.vatLines().get(21).vatAmount());
        this.assertBigDecimalEquals(new BigDecimal("145.20"), report.vatLines().get(21).totalAmount());
        this.assertBigDecimalEquals(new BigDecimal("20.00"), report.vatLines().get(10).baseAmount());
        this.assertBigDecimalEquals(new BigDecimal("2.00"), report.vatLines().get(10).vatAmount());
        this.assertBigDecimalEquals(new BigDecimal("22.00"), report.vatLines().get(10).totalAmount());
    }

    @Test
    void shouldFindReceivedBookByNumberRange() {
        Expense first = this.buildExpense(LocalDate.of(2026, 4, 10));
        Expense second = this.buildExpense(LocalDate.of(2026, 5, 15));
        when(this.expenseGateway.findInvoiceReceivedBook("2026", 2, 3, INVESTMENT_ASSET_THRESHOLD))
                .thenReturn(Stream.of(first, second));

        List<Expense> expenses = this.taxAgencyService.invoiceReceiveBook(2026, 2, 3);

        assertEquals(List.of(first, second), expenses);
        verify(this.expenseGateway).findInvoiceReceivedBook("2026", 2, 3, INVESTMENT_ASSET_THRESHOLD);
    }

    @Test
    void shouldReturnEmptyListWhenThereAreNoInvoices() {
        LocalDate fromDate = LocalDate.of(2026, 10, 1);
        LocalDate toDate = LocalDate.of(2026, 12, 31);
        when(this.invoiceGateway.findIssuedBetween(fromDate, toDate)).thenReturn(Stream.empty());

        assertEquals(List.of(), this.taxAgencyService.invoiceIssuedBook(2026, Quarter.T4));
        verify(this.invoiceGateway).findIssuedBetween(fromDate, toDate);
    }

    @Test
    void shouldBuildModel303WithIssuedByDateAndReceivedByNumberRange() {
        LocalDate fromDate = LocalDate.of(2026, 4, 1);
        LocalDate toDate = LocalDate.of(2026, 6, 30);
        Invoice firstInvoice = this.buildInvoice(31, LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 19),
                "12345678Z", "First Client", "100.00", "21.00");
        Invoice secondInvoice = this.buildInvoice(32, LocalDate.of(2026, 5, 20), LocalDate.of(2026, 5, 19),
                "87654321X", "Second Client", "200.00", "8.00");
        Expense currentExpense = this.buildExpense(LocalDate.of(2026, 4, 10), "50.00", 21, 100);
        Expense reducedVatCurrentExpense = this.buildExpense(LocalDate.of(2026, 5, 10), "25.00", 4, 100, new BigDecimal("50"));
        Expense investmentExpense = this.buildExpense(LocalDate.of(2026, 6, 10), "4000.00", 21, 10, new BigDecimal("25"));
        when(this.invoiceGateway.findIssuedBetween(fromDate, toDate))
                .thenReturn(Stream.of(firstInvoice, secondInvoice));
        when(this.expenseGateway.findInvoiceReceivedBook("2026", 31, 32, INVESTMENT_ASSET_THRESHOLD))
                .thenReturn(Stream.of(currentExpense, reducedVatCurrentExpense));
        when(this.expenseGateway.findInvoiceReceivedInvestmentBook("2026", 31, 32, INVESTMENT_ASSET_THRESHOLD))
                .thenReturn(Stream.of(investmentExpense));

        VatSummaryReport result = this.taxAgencyService.vatSummary(2026, Quarter.T2, 31, 32);

        this.assertVatSummary(new VatSummaryReport(
                new BigDecimal("300.00"),
                new BigDecimal("29.00"),
                new BigDecimal("62.50"),
                new BigDecimal("11.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("210.00")
        ), result);
        verify(this.invoiceGateway).findIssuedBetween(fromDate, toDate);
        verify(this.expenseGateway).findInvoiceReceivedBook("2026", 31, 32, INVESTMENT_ASSET_THRESHOLD);
        verify(this.expenseGateway).findInvoiceReceivedInvestmentBook("2026", 31, 32, INVESTMENT_ASSET_THRESHOLD);
    }

    @Test
    void shouldAddClosedInvoiceExpensesToVatSummaryIssuedAmounts() {
        LocalDate fromDate = LocalDate.of(2026, 4, 1);
        LocalDate toDate = LocalDate.of(2026, 6, 30);
        Invoice closedInvoice = this.buildInvoice(31, LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 19),
                "12345678Z", "First Client", "100.00", "21.00");
        closedInvoice.setClosed(true);
        closedInvoice.setBaseExpense(new BigDecimal("70.00"));
        closedInvoice.setVatExpense(new BigDecimal("10.30"));
        Invoice openInvoice = this.buildInvoice(32, LocalDate.of(2026, 5, 20), LocalDate.of(2026, 5, 19),
                "87654321X", "Second Client", "200.00", "42.00");
        openInvoice.setClosed(false);
        openInvoice.setBaseExpense(new BigDecimal("999.00"));
        openInvoice.setVatExpense(new BigDecimal("999.00"));
        when(this.invoiceGateway.findIssuedBetween(fromDate, toDate))
                .thenReturn(Stream.of(closedInvoice, openInvoice));
        when(this.expenseGateway.findInvoiceReceivedBook("2026", 31, 32, INVESTMENT_ASSET_THRESHOLD))
                .thenReturn(Stream.empty());
        when(this.expenseGateway.findInvoiceReceivedInvestmentBook("2026", 31, 32, INVESTMENT_ASSET_THRESHOLD))
                .thenReturn(Stream.empty());

        VatSummaryReport result = this.taxAgencyService.vatSummary(2026, Quarter.T2, 31, 32);

        this.assertVatSummary(new VatSummaryReport(
                new BigDecimal("370.00"),
                new BigDecimal("73.30"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        ), result);
        verify(this.invoiceGateway).findIssuedBetween(fromDate, toDate);
        verify(this.expenseGateway).findInvoiceReceivedBook("2026", 31, 32, INVESTMENT_ASSET_THRESHOLD);
        verify(this.expenseGateway).findInvoiceReceivedInvestmentBook("2026", 31, 32, INVESTMENT_ASSET_THRESHOLD);
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
        return this.buildExpense(issueDate, baseAmount, vatRate, depreciationRate, null);
    }

    private Expense buildExpense(LocalDate issueDate, String baseAmount, int vatRate, int depreciationRate,
                                 BigDecimal deductibleAmount) {
        return this.buildExpense(issueDate, baseAmount, vatRate, depreciationRate, deductibleAmount, null);
    }

    private Expense buildExpense(LocalDate issueDate, String baseAmount, int vatRate, int depreciationRate,
                                 BigDecimal deductibleAmount, BigDecimal withholdingTax) {
        return Expense.builder()
                .id(UUID.randomUUID())
                .issueDate(issueDate)
                .baseAmount(new BigDecimal(baseAmount))
                .deductibleAmount(deductibleAmount)
                .vatRate(vatRate)
                .supplier(SupplierInfo.builder()
                        .name("Supplier")
                        .identity("B10000000")
                        .build())
                .taxCategory(TaxCategory.OTROS)
                .depreciationRate(depreciationRate)
                .withholdingTax(withholdingTax)
                .build();
    }

    private void assertVatSummary(VatSummaryReport expected, VatSummaryReport actual) {
        this.assertBigDecimalEquals(expected.invoiceIssuedBase(), actual.invoiceIssuedBase());
        this.assertBigDecimalEquals(expected.invoiceIssuedVat(), actual.invoiceIssuedVat());
        this.assertBigDecimalEquals(expected.invoiceReceivedCurrentBase(), actual.invoiceReceivedCurrentBase());
        this.assertBigDecimalEquals(expected.invoiceReceivedCurrentVat(), actual.invoiceReceivedCurrentVat());
        this.assertBigDecimalEquals(expected.invoiceReceivedInvestmentBase(), actual.invoiceReceivedInvestmentBase());
        this.assertBigDecimalEquals(expected.invoiceReceivedInvestmentVat(), actual.invoiceReceivedInvestmentVat());
    }

    private void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual));
    }
}
