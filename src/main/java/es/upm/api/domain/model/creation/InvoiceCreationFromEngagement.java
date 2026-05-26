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
import java.util.stream.Collectors;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceCreationFromEngagement {
    @NotNull
    private UUID engagementId;

    private Boolean closeEngagement;

    @NotEmpty
    @Valid
    private List<LegalProcedureCreation> legalProcedures;

    @NotEmpty
    @Valid
    private List<InvoiceBillingPercentageCreation> billingPercentages;

    public BigDecimal totalBudget() {
        return legalProcedures.stream()
                .map(LegalProcedureCreation::getBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String buildProceduresText() {
        return legalProcedures.stream()
                .map(procedure -> {
                    String header = procedure.getTitle() + " - " +
                            procedure.getBudget().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() + " €";
                    String tasks = procedure.getLegalTasks().stream()
                            .map(task -> "- " + task)
                            .collect(Collectors.joining(System.lineSeparator()));
                    return header + System.lineSeparator() + tasks;
                })
                .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));
    }

}
