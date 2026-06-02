package es.upm.api.domain.model.creation;

import es.upm.miw.validations.ListNotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LegalProcedure {
    @NotBlank
    private String title;

    @PositiveOrZero
    private BigDecimal budget;

    @ListNotEmpty
    private List<String> legalTasks;
}
