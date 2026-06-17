package es.upm.api.adapter.in.resources;

import es.upm.api.adapter.in.resources.dtos.InvoiceBookDto;
import es.upm.api.adapter.in.resources.dtos.Model130Dto;
import es.upm.api.adapter.in.resources.dtos.Model303Dto;
import es.upm.api.domain.services.TaxAgencyService;
import es.upm.miw.security.Security;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@PreAuthorize(Security.ADMIN_MANAGER_OPERATOR)
@RestController
@RequestMapping(TaxAgencyResource.TAX_AGENCY)
@RequiredArgsConstructor
public class TaxAgencyResource {
    public static final String TAX_AGENCY = "/tax-agency";
    public static final String INVOICE_ISSUED_BOOK = "/invoice-issued-book";
    public static final String RECEIVED_BOOK = "/received-book";
    public static final String MODEL_130 = "/models/130";
    public static final String MODEL_303 = "/models/303";

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
    public String receivedBook(@RequestParam int year, @RequestParam Quarter quarter,
                               @RequestParam int from, @RequestParam int to) {
        List<String> lines = this.taxAgencyService.invoiceReceiveBook(String.valueOf(year), from, to).stream()
                .map(expense -> InvoiceBookDto.from(expense, quarter))
                .map(InvoiceBookDto::toCsvLine)
                .toList();
        return String.join("\r\n", lines);
    }

    @GetMapping(MODEL_303)
    public Model303Dto model303(@RequestParam int year, @RequestParam Quarter quarter,
                                @RequestParam int from, @RequestParam int to) {
        return new Model303Dto(year, quarter, this.taxAgencyService.vatSummary(String.valueOf(year), from, to));
    }

    @GetMapping(MODEL_130)
    public Model130Dto model130(@RequestParam int year, @RequestParam Quarter quarter, @RequestParam int to) {
        return new Model130Dto(
                year, quarter, this.taxAgencyService.netIncomeBreakdown(String.valueOf(year), to, quarter.toDate(year)));
    }
}
