package es.upm.api.infrastructure.resources.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import es.upm.api.domain.model.validations.AtLeastOneInvoiceItem;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@AtLeastOneInvoiceItem
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceUpdateRequest {

    @NotNull
    private UUID engagementId;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @NotNull
    private List<UUID> expenseIds;

    @NotNull
    private List<UUID> incomeIds;
}
