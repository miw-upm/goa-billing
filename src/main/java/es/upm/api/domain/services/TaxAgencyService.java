package es.upm.api.domain.services;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.report.VatSummary;
import es.upm.api.domain.ports.out.billing.ExpenseGateway;
import es.upm.api.domain.ports.out.billing.InvoiceGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class TaxAgencyService {
    private static final BigDecimal INVESTMENT_ASSET_THRESHOLD = new BigDecimal("3005.06");
    private final InvoiceGateway invoiceGateway;
    private final ExpenseGateway expenseGateway;

    public List<Invoice> invoiceIssuedBook(LocalDate fromDate, LocalDate toDate) {
        return this.invoiceGateway.findIssuedBetween(fromDate, toDate)
                .toList();
    }

    public List<Expense> invoiceReceiveBook(LocalDate fromDate, LocalDate toDate) {
        return this.findInvoiceReceivedBook(fromDate, toDate, INVESTMENT_ASSET_THRESHOLD);
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
                this.sum(invoiceReceivedCurrentBook, Expense::getBaseAmount),
                this.sum(invoiceReceivedCurrentBook, Expense::vatAmount),
                this.sum(invoiceReceivedInvestmentBook, Expense::getBaseAmount),
                this.sum(invoiceReceivedInvestmentBook, Expense::vatAmount)
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
}
