package es.upm.api.domain.services;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.report.InvoiceBookReport;
import es.upm.api.domain.model.report.NetIncomeBreakdownReport;
import es.upm.api.domain.model.report.Quarter;
import es.upm.api.domain.model.report.VatSummaryReport;
import es.upm.api.domain.ports.out.billing.ExpenseGateway;
import es.upm.api.domain.ports.out.billing.InvoiceGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class TaxAgencyService {
    private static final int SCALE = 6;
    private static final BigDecimal INVESTMENT_ASSET_THRESHOLD = new BigDecimal("3005.06");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWELVE = new BigDecimal("12");
    private final InvoiceGateway invoiceGateway;
    private final ExpenseGateway expenseGateway;

    public List<InvoiceBookReport> invoiceIssuedBook(int year, Quarter quarter) {
        return this.invoiceGateway.findIssuedBetween(quarter.fromDate(year), quarter.toDate(year))
                .map(InvoiceBookReport::from)
                .toList();
    }

    public List<Expense> invoiceReceiveBook(int year, int fromNumber, int toNumber) {
        return this.expenseGateway.findInvoiceReceivedBook(
                        String.valueOf(year), fromNumber, toNumber, INVESTMENT_ASSET_THRESHOLD)
                .toList();
    }

    public VatSummaryReport vatSummary(int year, Quarter quarter, int fromNumber, int toNumber) {
        String series = String.valueOf(year);
        List<Invoice> invoiceIssuedBook = this.invoiceGateway.findIssuedBetween(quarter.fromDate(year), quarter.toDate(year))
                .toList();
        List<Expense> invoiceReceivedCurrentBook = this.invoiceReceiveBook(year, fromNumber, toNumber);
        List<Expense> invoiceReceivedInvestmentBook = this.expenseGateway
                .findInvoiceReceivedInvestmentBook(series, fromNumber, toNumber, INVESTMENT_ASSET_THRESHOLD)
                .toList();
        return this.vatSummary(invoiceIssuedBook, invoiceReceivedCurrentBook, invoiceReceivedInvestmentBook);
    }

    private VatSummaryReport vatSummary(
            List<Invoice> invoiceIssuedBook,
            List<Expense> invoiceReceivedCurrentBook,
            List<Expense> invoiceReceivedInvestmentBook
    ) {
        return new VatSummaryReport(
                this.sum(invoiceIssuedBook, Invoice::getBaseAmount),
                this.sum(invoiceIssuedBook, Invoice::getVatAmount),
                this.sum(invoiceReceivedCurrentBook, Expense::deductibleBaseAmount),
                this.sum(invoiceReceivedCurrentBook, Expense::deductibleVatAmount),
                this.sum(invoiceReceivedInvestmentBook, Expense::deductibleBaseAmount),
                this.sum(invoiceReceivedInvestmentBook, Expense::deductibleVatAmount)
        );
    }

    public NetIncomeBreakdownReport netIncomeBreakdown(String series, int toNumber, LocalDate toDate) {
        List<Invoice> invoiceIssuedBook = this.invoiceGateway.findIssuedBetween(series, 1, toNumber)
                .toList();
        List<Expense> currentExpensesBook = this.expenseGateway.findCurrentExpensesBook(series, 1, toNumber)
                .toList();
        List<Expense> investmentAssets = this.expenseGateway.findInvestmentAssetsUntil(series, toNumber)
                .toList();
        return this.netIncomeBreakdown(invoiceIssuedBook, currentExpensesBook, investmentAssets, toDate);
    }

    private NetIncomeBreakdownReport netIncomeBreakdown(
            List<Invoice> invoiceIssuedBook,
            List<Expense> currentExpensesBook,
            List<Expense> investmentAssets,
            LocalDate toDate
    ) {
        return new NetIncomeBreakdownReport(
                this.sum(invoiceIssuedBook, Invoice::getBaseAmount),
                this.sum(currentExpensesBook, Expense::deductibleBaseAmount),
                this.sum(investmentAssets, investmentAsset -> this.investmentAmortization(investmentAsset, toDate)),
                this.sum(currentExpensesBook,
                        expense -> expense.getWithholdingTax() == null ? BigDecimal.ZERO : expense.getWithholdingTax())
        );
    }

    private <T> BigDecimal sum(List<T> values, Function<T, BigDecimal> mapper) {
        return values.stream()
                .map(mapper)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal investmentAmortization(Expense investmentAsset, LocalDate toDate) {
        BigDecimal baseAmount = investmentAsset.deductibleBaseAmount();
        BigDecimal amortizedBeforeYear = this.amortizationAmount(
                baseAmount, investmentAsset.getDepreciationRate(),
                this.monthsBeforeYear(investmentAsset.getIssueDate(), toDate)
        );
        BigDecimal remainingAmount = baseAmount.subtract(amortizedBeforeYear).max(BigDecimal.ZERO);
        BigDecimal currentYearAmortization = this.amortizationAmount(
                baseAmount, investmentAsset.getDepreciationRate(),
                this.monthsInCurrentYear(investmentAsset.getIssueDate(), toDate)
        );
        return currentYearAmortization.min(remainingAmount);
    }

    private BigDecimal amortizationAmount(BigDecimal baseAmount, int depreciationRate, int months) {
        return baseAmount
                .multiply(BigDecimal.valueOf(depreciationRate))
                .multiply(BigDecimal.valueOf(months))
                .divide(HUNDRED.multiply(TWELVE), SCALE, RoundingMode.HALF_UP);
    }

    private int monthsBeforeYear(LocalDate issueDate, LocalDate toDate) {
        if (issueDate.getYear() >= toDate.getYear()) {
            return 0;
        }
        int firstYearMonths = 13 - issueDate.getMonthValue();
        int fullYearsBetween = toDate.getYear() - issueDate.getYear() - 1;
        return firstYearMonths + fullYearsBetween * 12;
    }

    private int monthsInCurrentYear(LocalDate issueDate, LocalDate toDate) {
        if (issueDate.getYear() == toDate.getYear()) {
            return toDate.getMonthValue() - issueDate.getMonthValue() + 1;
        }
        return toDate.getMonthValue();
    }
}
