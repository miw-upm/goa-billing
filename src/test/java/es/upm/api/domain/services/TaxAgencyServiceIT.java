package es.upm.api.domain.services;

import es.upm.api.domain.model.*;
import es.upm.api.domain.model.report.NetIncomeBreakdown;
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
    void shouldFindReceivedBookByNumberRange() {
        Expense first = this.buildExpense(LocalDate.of(2026, 4, 10));
        Expense second = this.buildExpense(LocalDate.of(2026, 5, 15));
        when(this.expenseGateway.findInvoiceReceivedBook("2026", 2, 3, INVESTMENT_ASSET_THRESHOLD))
                .thenReturn(Stream.of(first, second));

        List<Expense> expenses = this.taxAgencyService.invoiceReceiveBook("2026", 2, 3);

        assertEquals(List.of(first, second), expenses);
        verify(this.expenseGateway).findInvoiceReceivedBook("2026", 2, 3, INVESTMENT_ASSET_THRESHOLD);
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

        VatSummary result = this.taxAgencyService.vatSummary(fromDate, toDate, "2026", 31, 32);

        this.assertVatSummary(new VatSummary(
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
    void shouldBuildNetIncomeBreakdown() {
        LocalDate toDate = LocalDate.of(2026, 6, 30);
        LocalDate fromYearDate = LocalDate.of(2026, 1, 1);
        Invoice firstInvoice = this.buildInvoice(31, LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 19),
                "12345678Z", "First Client", "100.00", "21.00");
        Invoice secondInvoice = this.buildInvoice(32, LocalDate.of(2026, 5, 20), LocalDate.of(2026, 5, 19),
                "87654321X", "Second Client", "200.00", "42.00");
        Expense currentExpense = this.buildExpense(LocalDate.of(2026, 4, 10), "50.00", 21, 100,
                null, new BigDecimal("7.50"));
        Expense reducedCurrentExpense = this.buildExpense(LocalDate.of(2026, 5, 10), "25.00", 4, 100,
                new BigDecimal("50"), new BigDecimal("2.50"));
        Expense currentYearInvestment = this.buildExpense(LocalDate.of(2026, 2, 12), "12000.00", 21, 12);
        Expense smallCurrentYearInvestment = this.buildExpense(LocalDate.of(2026, 5, 7), "1000.00", 21, 50);
        Expense previousYearInvestment = this.buildExpense(LocalDate.of(2025, 6, 12), "6000.00", 21, 20);
        Expense alreadyAmortizedInvestment = this.buildExpense(LocalDate.of(2020, 1, 12), "6000.00", 21, 50);
        when(this.invoiceGateway.findIssuedBetween(fromYearDate, toDate))
                .thenReturn(Stream.of(firstInvoice, secondInvoice));
        when(this.expenseGateway.findCurrentExpensesBook(fromYearDate, toDate))
                .thenReturn(Stream.of(currentExpense, reducedCurrentExpense));
        when(this.expenseGateway.findInvestmentAssetsUntil(toDate))
                .thenReturn(Stream.of(currentYearInvestment, smallCurrentYearInvestment,
                        previousYearInvestment, alreadyAmortizedInvestment));

        NetIncomeBreakdown result = this.taxAgencyService.netIncomeBreakdown(toDate);

        this.assertNetIncomeBreakdown(new NetIncomeBreakdown(
                new BigDecimal("300.00"),
                new BigDecimal("62.50"),
                new BigDecimal("1283.333333"),
                new BigDecimal("10.00")
        ), result);
        verify(this.invoiceGateway).findIssuedBetween(fromYearDate, toDate);
        verify(this.expenseGateway).findCurrentExpensesBook(fromYearDate, toDate);
        verify(this.expenseGateway).findInvestmentAssetsUntil(toDate);
    }

    @Test
    void shouldBuildNetIncomeBreakdownByNumberRange() {
        LocalDate toDate = LocalDate.of(2026, 6, 30);
        Invoice firstInvoice = this.buildInvoice(31, LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 19),
                "12345678Z", "First Client", "100.00", "21.00");
        Invoice secondInvoice = this.buildInvoice(32, LocalDate.of(2026, 5, 20), LocalDate.of(2026, 5, 19),
                "87654321X", "Second Client", "200.00", "42.00");
        Expense currentExpense = this.buildExpense(LocalDate.of(2026, 4, 10), "50.00", 21, 100,
                null, new BigDecimal("7.50"));
        Expense reducedCurrentExpense = this.buildExpense(LocalDate.of(2026, 5, 10), "25.00", 4, 100,
                new BigDecimal("50"), new BigDecimal("2.50"));
        Expense currentYearInvestment = this.buildExpense(LocalDate.of(2026, 2, 12), "12000.00", 21, 12);
        Expense smallCurrentYearInvestment = this.buildExpense(LocalDate.of(2026, 5, 7), "1000.00", 21, 50);
        Expense previousYearInvestment = this.buildExpense(LocalDate.of(2025, 6, 12), "6000.00", 21, 20);
        Expense alreadyAmortizedInvestment = this.buildExpense(LocalDate.of(2020, 1, 12), "6000.00", 21, 50);
        when(this.invoiceGateway.findIssuedBetween("2026", 1, 3))
                .thenReturn(Stream.of(firstInvoice, secondInvoice));
        when(this.expenseGateway.findCurrentExpensesBook("2026", 1, 3))
                .thenReturn(Stream.of(currentExpense, reducedCurrentExpense));
        when(this.expenseGateway.findInvestmentAssetsUntil("2026", 3))
                .thenReturn(Stream.of(currentYearInvestment, smallCurrentYearInvestment,
                        previousYearInvestment, alreadyAmortizedInvestment));

        NetIncomeBreakdown result = this.taxAgencyService.netIncomeBreakdown("2026", 3, toDate);

        this.assertNetIncomeBreakdown(new NetIncomeBreakdown(
                new BigDecimal("300.00"),
                new BigDecimal("62.50"),
                new BigDecimal("1283.333333"),
                new BigDecimal("10.00")
        ), result);
        verify(this.invoiceGateway).findIssuedBetween("2026", 1, 3);
        verify(this.expenseGateway).findCurrentExpensesBook("2026", 1, 3);
        verify(this.expenseGateway).findInvestmentAssetsUntil("2026", 3);
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

    private void assertVatSummary(VatSummary expected, VatSummary actual) {
        this.assertBigDecimalEquals(expected.invoiceIssuedBase(), actual.invoiceIssuedBase());
        this.assertBigDecimalEquals(expected.invoiceIssuedVat(), actual.invoiceIssuedVat());
        this.assertBigDecimalEquals(expected.invoiceReceivedCurrentBase(), actual.invoiceReceivedCurrentBase());
        this.assertBigDecimalEquals(expected.invoiceReceivedCurrentVat(), actual.invoiceReceivedCurrentVat());
        this.assertBigDecimalEquals(expected.invoiceReceivedInvestmentBase(), actual.invoiceReceivedInvestmentBase());
        this.assertBigDecimalEquals(expected.invoiceReceivedInvestmentVat(), actual.invoiceReceivedInvestmentVat());
    }

    private void assertNetIncomeBreakdown(NetIncomeBreakdown expected, NetIncomeBreakdown actual) {
        this.assertBigDecimalEquals(expected.income(), actual.income());
        this.assertBigDecimalEquals(expected.currentExpenses(), actual.currentExpenses());
        this.assertBigDecimalEquals(expected.investmentAmortization(), actual.investmentAmortization());
        this.assertBigDecimalEquals(expected.withholdings(), actual.withholdings());
    }

    private void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual));
    }
}
