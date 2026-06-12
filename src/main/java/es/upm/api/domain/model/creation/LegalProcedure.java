package es.upm.api.domain.model.creation;

import es.upm.miw.validations.ListNotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LegalProcedure {
    @NotBlank
    private String title;

    private BigDecimal budget;
    private String budgetProposal;

    @ListNotEmpty
    private List<String> legalTasks;

    public String buildFormatBudget() {
        if (budget == null) {
            return budgetProposal + " (+ IVA)";
        } else {
            return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-ES")).format(budget) + " (+ IVA)";
        }
    }
}
