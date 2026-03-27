package es.upm.api.infrastructure.resources;

import es.upm.api.domain.model.Income;
import es.upm.api.domain.services.IncomeService;
import es.upm.api.infrastructure.resources.dtos.IncomeCreateRequest;
import es.upm.api.infrastructure.resources.dtos.IncomeUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.stream.Stream;

@RestController
@RequestMapping(IncomeResource.INCOMES)
public class IncomeResource {

    public static final String INCOMES = "/incomes";

    private final IncomeService incomeService;

    public IncomeResource(IncomeService incomeService) {
        this.incomeService = incomeService;
    }

    @PostMapping
    @Operation(summary = "Create income")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize(Security.ADMIN_MANAGER_OPERATOR)
    public Income create(@Valid @RequestBody IncomeCreateRequest request) {
        return this.incomeService.create(
                Income.builder()
                        .engagementId(request.getEngagementId())
                        .userId(request.getUserId())
                        .amount(request.getAmount())
                        .date(request.getDate())
                        .build()
        );
    }

    @GetMapping
    @Operation(summary = "List incomes")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize(Security.ADMIN_MANAGER_OPERATOR)
    public Stream<Income> findAll(@RequestParam(required = false) UUID engagementId) {
        return this.incomeService.findAll(engagementId);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update income")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize(Security.ADMIN_MANAGER_OPERATOR)
    public Income update(
            @PathVariable("id") java.util.UUID id,
            @Valid @RequestBody IncomeUpdateRequest request) {
        return this.incomeService.update(
                id,
                Income.builder()
                        .engagementId(request.getEngagementId())
                        .userId(request.getUserId())
                        .amount(request.getAmount())
                        .date(request.getDate())
                        .build()
        );
    }
}