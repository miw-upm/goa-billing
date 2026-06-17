package es.upm.api.domain.services;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.report.NetIncomeBreakdown;
import es.upm.api.domain.model.report.VatSummary;
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

    public List<Invoice> invoiceIssuedBook(LocalDate fromDate, LocalDate toDate) {
        return this.invoiceGateway.findIssuedBetween(fromDate, toDate)
                .toList();
    }

    public List<Expense> invoiceReceiveBook(LocalDate fromDate, LocalDate toDate) {
        return this.findInvoiceReceivedBook(fromDate, toDate, INVESTMENT_ASSET_THRESHOLD);
    }

    public List<Expense> invoiceReceiveBook(String series, int fromNumber, int toNumber) {
        return this.expenseGateway.findInvoiceReceivedBook(series, fromNumber, toNumber, INVESTMENT_ASSET_THRESHOLD)
                .toList();
    }

    public List<Expense> findInvoiceReceivedBook(LocalDate fromDate, LocalDate toDate, BigDecimal taxableBaseThreshold) {
        return this.expenseGateway.findInvoiceReceivedBook(fromDate, toDate, taxableBaseThreshold)
                .toList();
    }

    public List<Expense> invoiceReceivedInvestmentBook(LocalDate fromDate, LocalDate toDate) {
        return this.expenseGateway.findInvoiceReceivedInvestmentBook(fromDate, toDate, INVESTMENT_ASSET_THRESHOLD)
                .toList();
    }

    public VatSummary vatSummary(LocalDate fromDate, LocalDate toDate) {
        List<Invoice> invoiceIssuedBook = this.invoiceIssuedBook(fromDate, toDate);
        List<Expense> invoiceReceivedCurrentBook = this.invoiceReceiveBook(fromDate, toDate);
        List<Expense> invoiceReceivedInvestmentBook = this.invoiceReceivedInvestmentBook(fromDate, toDate);
        return new VatSummary(
                this.sum(invoiceIssuedBook, Invoice::getBaseAmount),
                this.sum(invoiceIssuedBook, Invoice::getVatAmount),
                this.sum(invoiceReceivedCurrentBook, Expense::deductibleBaseAmount),
                this.sum(invoiceReceivedCurrentBook, Expense::deductibleVatAmount),
                this.sum(invoiceReceivedInvestmentBook, Expense::deductibleBaseAmount),
                this.sum(invoiceReceivedInvestmentBook, Expense::deductibleVatAmount)
        );
    }

    public NetIncomeBreakdown netIncomeBreakdown(LocalDate toDate) {
        LocalDate fromDate = LocalDate.of(toDate.getYear(), 1, 1);
        List<Invoice> invoiceIssuedBook = this.invoiceIssuedBook(fromDate, toDate);
        List<Expense> currentExpensesBook = this.expenseGateway.findCurrentExpensesBook(fromDate, toDate)
                .toList();
        List<Expense> investmentAssets = this.expenseGateway.findInvestmentAssetsUntil(toDate)
                .toList();
        return new NetIncomeBreakdown(
                this.sum(invoiceIssuedBook, Invoice::getBaseAmount),
                this.sum(currentExpensesBook, Expense::deductibleBaseAmount),
                this.sum(investmentAssets, investmentAsset -> this.investmentAmortization(investmentAsset, toDate)),
                this.sum(currentExpensesBook,
                        expense -> expense.getWithholdingTax() == null ? BigDecimal.ZERO : expense.getWithholdingTax())
        );
    }

    public int countInvoiceReceiveBook(LocalDate fromDate, LocalDate toDate) {
        return this.countInvoiceReceivedBook(fromDate, toDate, INVESTMENT_ASSET_THRESHOLD);
    }

    public int countInvoiceReceivedBook(LocalDate fromDate, LocalDate toDate, BigDecimal taxableBaseThreshold) {
        return Math.toIntExact(this.expenseGateway.countInvoiceReceivedBook(fromDate, toDate, taxableBaseThreshold));
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
