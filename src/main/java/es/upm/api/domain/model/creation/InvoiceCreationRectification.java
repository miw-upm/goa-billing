package es.upm.api.domain.model.creation;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceCreationRectification {
    @NotNull
    private String series;

    @NotNull
    private Integer number;

    @NotNull
    private String reason;

}
