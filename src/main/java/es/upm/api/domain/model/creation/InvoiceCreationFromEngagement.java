package es.upm.api.domain.model.creation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceCreationFromEngagement {
    @NotNull
    private UUID engagementId;

    private Boolean closeEngagement;

    private String concept;

    @NotEmpty
    @Valid
    private List<InvoiceLegalProcedure> legalProcedures;

    @NotEmpty
    @Valid
    private List<InvoiceBillingPercentageCreation> billingPercentages;

    public BigDecimal totalBudget() {
        return legalProcedures.stream()
                .map(InvoiceLegalProcedure::getBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
