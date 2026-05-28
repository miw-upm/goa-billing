package es.upm.api.adapter.in.resources;

import es.upm.api.adapter.in.resources.dtos.InvoiceCreationDto;
import es.upm.api.domain.model.BillingInfo;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.creation.InvoiceCreationFromEngagement;
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
    public static final String FROM_PAYMENTS = "/from-payments";
    public static final String FROM_ENGAGEMENT = "/from-engagement";


    private final InvoiceService invoiceService;

    @PostMapping
    public void create(@Valid @RequestBody InvoiceCreationDto creation) {
        Invoice invoice = Invoice.builder()
                .billingInfo(BillingInfo.builder()
                        .userId(creation.getUserId())
                        .concept(creation.getConcept())
                        .build())
                .baseAmount(creation.getBaseAmount())
                .discounts(creation.getDiscounts())
                .build();
        this.invoiceService.create(invoice);
    }

    @PostMapping(FROM_ENGAGEMENT)
    public void createFromEngagement(@RequestBody @Valid InvoiceCreationFromEngagement creation) {
        this.invoiceService.createFromEngagement(creation);
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

    @PreAuthorize(Security.ADMIN)
    @PostMapping("/{id}/emission")
    public void emission(@PathVariable UUID id) {
        this.invoiceService.emission(id);
    }

    @GetMapping(value = "/{id}/view", produces = {"application/pdf"})
    public byte[] view(@PathVariable UUID id) {
        return this.invoiceService.generatePdf(id);
    }

    @GetMapping
    public Stream<Invoice> find(@ModelAttribute InvoiceFindCriteria criteria) {
        return this.invoiceService.find(criteria);
    }
}
