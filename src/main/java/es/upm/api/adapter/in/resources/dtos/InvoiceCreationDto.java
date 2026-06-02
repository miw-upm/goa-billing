package es.upm.api.adapter.in.resources.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceCreationDto {
    @NotNull
    private UUID userId;
    @NotBlank
    private String concept;
    @NotNull
    @Positive
    private BigDecimal baseAmount;
    private BigDecimal baseExpense;
    private BigDecimal vatExpense;
}
