package es.upm.api.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OriginalInvoice {
    @NotNull
    private String series;

    @NotNull
    private Integer number;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate emissionDate;

    @NotBlank
    private String reason;
}
