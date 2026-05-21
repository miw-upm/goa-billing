package es.upm.api.infrastructure.resources;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.Income;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.criteria.InvoiceFindCriteria;
import es.upm.api.domain.model.InvoiceBreakdown;
import es.upm.api.domain.services.InvoiceService;
import es.upm.api.infrastructure.resources.dtos.InvoiceCreateRequest;
import es.upm.api.infrastructure.resources.dtos.InvoiceUpdateRequest;
import es.upm.miw.security.Security;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.stream.Stream;

@RestController
@RequestMapping(InvoiceResource.INVOICES)
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize(Security.ADMIN_MANAGER_OPERATOR)
public class InvoiceResource {

    public static final String INVOICES = "/invoices";

    private final InvoiceService invoiceService;

    public InvoiceResource(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    @Operation(summary = "Create invoice")
    public Invoice create(@Valid @RequestBody InvoiceCreateRequest request) {
        return this.invoiceService.create(
                Invoice.builder()
                        .engagementId(request.getEngagementId())
                        .date(request.getDate())
                        .expenses(request.getExpenseIds().stream()
                                .map(expenseId -> Expense.builder().id(expenseId).build())
                                .toList())
                        .incomes(request.getIncomeIds().stream()
                                .map(incomeId -> Income.builder().id(incomeId).build())
                                .toList())
                        .build()
        );
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update invoice")
    public Invoice update(@PathVariable UUID id, @Valid @RequestBody InvoiceUpdateRequest request) {
        return this.invoiceService.update(
                id,
                Invoice.builder()
                        .engagementId(request.getEngagementId())
                        .date(request.getDate())
                        .expenses(request.getExpenseIds().stream()
                                .map(expenseId -> Expense.builder().id(expenseId).build())
                                .toList())
                        .incomes(request.getIncomeIds().stream()
                                .map(incomeId -> Income.builder().id(incomeId).build())
                                .toList())
                        .build()
        );
    }

    @GetMapping
    @Operation(summary = "List invoices")
    public Stream<Invoice> findAll(@ModelAttribute InvoiceFindCriteria criteria) {
        return this.invoiceService.findAll(criteria);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Read invoice by id")
    public Invoice readById(@PathVariable UUID id) {
        return this.invoiceService.readById(id);
    }

    @GetMapping("/{id}/breakdown")
    @Operation(summary = "Get invoice breakdown")
    public InvoiceBreakdown getInvoiceBreakdown(@PathVariable UUID id) {
        return this.invoiceService.getInvoiceBreakdown(id);
    }
}
