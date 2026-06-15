package es.upm.api.domain.services;

import es.upm.api.adapter.in.resources.Quarter;
import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.ports.out.billing.ExpenseGateway;
import es.upm.api.domain.ports.out.billing.InvoiceGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaxAgencyService {
    private final InvoiceGateway invoiceGateway;
    private final ExpenseGateway expenseGateway;

    public List<Invoice> invoiceIssuedBook(LocalDate fromDate, LocalDate toDate) {
        return this.invoiceGateway.findIssuedBetween(fromDate, toDate)
                .toList();
    }

    public List<Expense> findByYearAndQuarter(int year, Quarter quarter) {
        return this.expenseGateway.findReceivedBook(quarter.fromDate(year), quarter.toDate(year))
                .toList();
    }

    public int countByYearBeforeQuarter(int year, Quarter quarter) {
        LocalDate fromDate = LocalDate.of(year, 1, 1);
        LocalDate toDate = quarter.fromDate(year).minusDays(1);
        if (toDate.isBefore(fromDate)) {
            return 0;
        }
        return Math.toIntExact(this.expenseGateway.countReceivedBook(fromDate, toDate));
    }
}
