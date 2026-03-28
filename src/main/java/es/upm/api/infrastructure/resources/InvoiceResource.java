package es.upm.api.infrastructure.resources;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.Income;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.services.InvoiceService;
import es.upm.api.infrastructure.resources.dtos.InvoiceCreateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping(InvoiceResource.INVOICES)
public class InvoiceResource {

    public static final String INVOICES = "/invoices";

    private final InvoiceService invoiceService;

    public InvoiceResource(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    @Operation(summary = "Create invoice")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize(Security.ADMIN_MANAGER_OPERATOR)
    public Invoice create(@Valid @RequestBody InvoiceCreateRequest request) {
        return this.invoiceService.create(
                Invoice.builder()
                        .engagementId(request.getEngagementId())
                        .date(request.getDate())
                        .expenses(request.getExpenseIds().stream()
                                .map(expenseId -> Expense.builder().id(expenseId).build())
                                .collect(Collectors.toList()))
                        .incomes(request.getIncomeIds().stream()
                                .map(incomeId -> Income.builder().id(incomeId).build())
                                .collect(Collectors.toList()))
                        .build()
        );
    }
}
