package es.upm.api.adapter.in.resources;

import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.criteria.InvoiceFindCriteria;
import es.upm.api.domain.services.InvoiceService;
import es.upm.miw.security.Security;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.stream.Stream;

@PreAuthorize(Security.ADMIN_MANAGER_OPERATOR)
@RestController
@RequestMapping(InvoiceResource.INVOICES)
@RequiredArgsConstructor
public class InvoiceResource {
    public static final String INVOICES = "/invoices";

    private final InvoiceService invoiceService;

    @PostMapping
    public Invoice create(@Valid @RequestBody Invoice request) {
        return this.invoiceService.create(request);
    }

    @GetMapping("/{id}")
    public Invoice read(@PathVariable UUID id) {
        return this.invoiceService.read(id);
    }

    @PutMapping("/{id}")
    public Invoice update(@PathVariable UUID id, @Valid @RequestBody Invoice request) {
        return this.invoiceService.update(id, request);
    }

    @PreAuthorize(Security.ADMIN)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        this.invoiceService.delete(id);
    }

    @GetMapping
    public Stream<Invoice> find(@ModelAttribute InvoiceFindCriteria criteria) {
        return this.invoiceService.find(criteria);
    }
}
