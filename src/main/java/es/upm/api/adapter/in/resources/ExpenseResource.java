package es.upm.api.adapter.in.resources;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.TaxCategory;
import es.upm.api.domain.model.criteria.ExpenseFindCriteria;
import es.upm.api.domain.services.ExpenseService;
import es.upm.miw.security.Security;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@PreAuthorize(Security.ADMIN_MANAGER_OPERATOR)
@RestController
@RequestMapping(ExpenseResource.EXPENSES)
@RequiredArgsConstructor
public class ExpenseResource {
    public static final String EXPENSES = "/expenses";

    private final ExpenseService expenseService;

    @PostMapping
    public void create(@Valid @RequestBody Expense expense) {
        this.expenseService.create(expense);
    }

    @GetMapping("/{id}")
    public Expense read(@PathVariable UUID id) {
        return this.expenseService.read(id);
    }

    @PutMapping("/{id}")
    public void update(@PathVariable UUID id, @Valid @RequestBody Expense expense) {
        this.expenseService.update(id, expense);
    }

    @PreAuthorize(Security.ADMIN)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        this.expenseService.delete(id);
    }

    @GetMapping
    public List<Expense> find(@ModelAttribute ExpenseFindCriteria criteria) {
        return this.expenseService.find(criteria).toList();
    }

    @GetMapping("/categories")
    public CategoryResponseDto categories() {
        return new CategoryResponseDto(Arrays.stream(TaxCategory.values())
                .map(TaxCategory::name)
                .toList());
    }
}
