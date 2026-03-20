package es.upm.api.infrastructure.resources;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.services.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    public Expense create(@Valid @RequestBody Expense expense) {
        return this.expenseService.create(expense);
    }
}