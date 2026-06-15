package es.upm.api.adapter.in.resources;

import es.upm.api.adapter.in.resources.dtos.InvoiceIssuedBookDto;
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

    private final TaxAgencyService taxAgencyService;

    @GetMapping(value = INVOICE_ISSUED_BOOK, produces = {"text/csv"})
    public String invoiceIssuedBook(@RequestParam int year, @RequestParam Quarter quarter) {
        List<String> lines = this.taxAgencyService
                .invoiceIssuedBook(quarter.fromDate(year), quarter.toDate(year)).stream()
                .map(InvoiceIssuedBookDto::from)
                .map(InvoiceIssuedBookDto::toCsvLine)
                .toList();
        return lines.isEmpty() ? "" : String.join("\r\n", lines) + "\r\n";
    }
}
