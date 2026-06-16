package es.upm.api.adapter.in.resources;

import es.upm.api.adapter.in.resources.dtos.InvoiceBookDto;
import es.upm.api.domain.services.TaxAgencyService;
import es.upm.miw.security.Security;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

@PreAuthorize(Security.ADMIN_MANAGER_OPERATOR)
@RestController
@RequestMapping(TaxAgencyResource.TAX_AGENCY)
@RequiredArgsConstructor
public class TaxAgencyResource {
    public static final String TAX_AGENCY = "/tax-agency";
    public static final String INVOICE_ISSUED_BOOK = "/invoice-issued-book";
    public static final String RECEIVED_BOOK = "/received-book";

    private final TaxAgencyService taxAgencyService;

    @GetMapping(value = INVOICE_ISSUED_BOOK, produces = {"text/csv"})
    public String invoiceIssuedBook(@RequestParam int year, @RequestParam Quarter quarter) {
        List<String> lines = this.taxAgencyService
                .invoiceIssuedBook(quarter.fromDate(year), quarter.toDate(year)).stream()
                .map(InvoiceBookDto::from)
                .map(InvoiceBookDto::toCsvLine)
                .toList();
        return String.join("\r\n", lines);
    }

    @GetMapping(value = RECEIVED_BOOK, produces = {"text/csv"})
    public String receivedBook(@RequestParam int year, @RequestParam Quarter quarter) {
        LocalDate fromDate = quarter.fromDate(year);
        LocalDate toDate = quarter.toDate(year);
        int offset = quarter == Quarter.T1
                ? 0
                : this.taxAgencyService.countInvoiceReceiveBook(LocalDate.of(year, 1, 1), fromDate.minusDays(1));
        var expenses = this.taxAgencyService.invoiceReceiveBook(fromDate, toDate);
        List<String> lines = IntStream.range(0, expenses.size())
                .mapToObj(index -> InvoiceBookDto.from(expenses.get(index), offset + index + 1))
                .map(InvoiceBookDto::toCsvLine)
                .toList();
        return String.join("\r\n", lines);
    }
}
