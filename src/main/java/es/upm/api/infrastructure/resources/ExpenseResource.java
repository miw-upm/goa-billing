package es.upm.api.infrastructure.resources;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.ExpenseFindCriteria;
import es.upm.api.domain.services.ExpenseService;
import es.upm.api.infrastructure.resources.dtos.ExpenseCreateRequest;
import es.upm.api.infrastructure.resources.dtos.ExpenseUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.stream.Stream;

@RestController
@RequestMapping(ExpenseResource.EXPENSES)
@PreAuthorize(Security.ADMIN_MANAGER_OPERATOR)
public class ExpenseResource {

    public static final String EXPENSES = "/expenses";

    private final ExpenseService expenseService;

    public ExpenseResource(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    @Operation(summary = "Create expense")
    public Expense create(@Valid @RequestBody ExpenseCreateRequest request) {
        Expense expense = Expense.builder()
                .engagementId(request.getEngagementId())
                .amount(request.getAmount())
                .date(request.getDate())
                .description(request.getDescription())
                .build();
        return this.expenseService.create(expense);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update expense")
    public Expense update(@PathVariable UUID id, @Valid @RequestBody ExpenseUpdateRequest request) {
        Expense expense = Expense.builder()
                .engagementId(request.getEngagementId())
                .amount(request.getAmount())
                .date(request.getDate())
                .description(request.getDescription())
                .build();
        return this.expenseService.update(id, expense);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Read expense by id")
    public Expense readById(@PathVariable UUID id) {
        return this.expenseService.readById(id);
    }

    @GetMapping
    public Stream<Expense> findAll(@ModelAttribute ExpenseFindCriteria criteria) {
        return this.expenseService.findAll(criteria);
    }
}
