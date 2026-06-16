package es.upm.api.domain.services;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.ports.out.billing.ExpenseGateway;
import es.upm.api.domain.ports.out.billing.InvoiceGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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

    public int countInvoiceReceiveBook(LocalDate fromDate, LocalDate toDate) {
        return this.countInvoiceReceivedBook(fromDate, toDate, INVESTMENT_ASSET_THRESHOLD);
    }

    public int countInvoiceReceivedBook(LocalDate fromDate, LocalDate toDate, BigDecimal taxableBaseThreshold) {
        return Math.toIntExact(this.expenseGateway.countInvoiceReceivedBook(fromDate, toDate, taxableBaseThreshold));
    }
}
