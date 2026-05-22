package es.upm.api.adapter.in.resources;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.criteria.ExpenseFindCriteria;
import es.upm.api.domain.services.ExpenseService;
import es.upm.miw.security.Security;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.stream.Stream;

@PreAuthorize(Security.ADMIN_MANAGER_OPERATOR)
@RestController
@RequestMapping(ExpenseResource.EXPENSES)
@RequiredArgsConstructor
public class ExpenseResource {
    public static final String EXPENSES = "/expenses";

    private final ExpenseService expenseService;

    @PostMapping
    public Expense create(@Valid @RequestBody Expense request) {
        return this.expenseService.create(request);
    }

    @GetMapping("/{id}")
    public Expense read(@PathVariable UUID id) {
        return this.expenseService.read(id);
    }

    @PutMapping("/{id}")
    public Expense update(@PathVariable UUID id, @Valid @RequestBody Expense request) {
        return this.expenseService.update(id, request);
    }

    @PreAuthorize(Security.ADMIN)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        this.expenseService.delete(id);
    }

    @GetMapping
    public Stream<Expense> find(@ModelAttribute ExpenseFindCriteria criteria) {
        return this.expenseService.find(criteria);
    }
}
