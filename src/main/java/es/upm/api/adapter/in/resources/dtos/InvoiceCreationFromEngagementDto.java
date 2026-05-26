package es.upm.api.adapter.in.resources.dtos;

import es.upm.api.domain.model.InvoiceBillingPercentageCreation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceCreationFromEngagementDto {
    @NotNull
    private UUID engagementId;

    @NotEmpty
    @Valid
    private List<LegalProcedureCreationDto> legalProcedures;

    @NotEmpty
    @Valid
    private List<InvoiceBillingPercentageCreation> billingPercentages;

    public BigDecimal totalBudget() {
        return Optional.ofNullable(legalProcedures)
                .orElse(List.of())
                .stream()
                .map(LegalProcedureCreationDto::getBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
