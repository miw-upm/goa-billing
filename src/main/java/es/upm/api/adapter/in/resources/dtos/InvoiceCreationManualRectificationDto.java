package es.upm.api.adapter.in.resources.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import es.upm.api.domain.model.OriginalInvoice;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceCreationManualRectificationDto {
    @NotNull
    @Valid
    private OriginalInvoice originalInvoice;

    @NotBlank
    private String concept;

    @NotNull
    private UUID userId;

    private BigDecimal percentage;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate operationDate;

    @NotNull
    private BigDecimal baseAmount;

    @NotNull
    private BigDecimal vatAmount;

    @NotNull
    private BigDecimal vatRate;

    private BigDecimal baseExpense;

    private BigDecimal vatExpense;
}
