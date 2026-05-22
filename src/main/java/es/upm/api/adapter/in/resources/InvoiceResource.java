package es.upm.api.adapter.in.resources;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.Income;
import es.upm.api.domain.model.InvoiceOld;
import es.upm.api.domain.model.criteria.InvoiceFindCriteria;
import es.upm.api.domain.model.InvoiceBreakdown;
import es.upm.api.domain.services.InvoiceService;
import es.upm.api.adapter.in.resources.dtos.InvoiceCreationDto;
import es.upm.api.adapter.in.resources.dtos.InvoiceUpdatingDto;
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
    public InvoiceOld create(@Valid @RequestBody InvoiceCreationDto request) {
        return this.invoiceService.create(
                InvoiceOld.builder()
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
    public InvoiceOld update(@PathVariable UUID id, @Valid @RequestBody InvoiceUpdatingDto request) {
        return this.invoiceService.update(
                id,
                InvoiceOld.builder()
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
    public Stream<InvoiceOld> findAll(@ModelAttribute InvoiceFindCriteria criteria) {
        return this.invoiceService.findAll(criteria);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Read invoice by id")
    public InvoiceOld readById(@PathVariable UUID id) {
        return this.invoiceService.readById(id);
    }

    @GetMapping("/{id}/breakdown")
    @Operation(summary = "Get invoice breakdown")
    public InvoiceBreakdown getInvoiceBreakdown(@PathVariable UUID id) {
        return this.invoiceService.getInvoiceBreakdown(id);
    }
}
