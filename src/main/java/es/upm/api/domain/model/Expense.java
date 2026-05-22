package es.upm.api.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import es.upm.api.domain.model.external.EngagementSnapshot;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class Expense {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UUID id;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDate date;

    @NotNull
    private EngagementSnapshot engagement;

    @NotNull
    @Positive
    private BigDecimal baseAmount;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal vatRate = new BigDecimal("21");

    @NotBlank
    private String supplier;

    @NotBlank
    private String supplierIdentity;

    @NotNull
    private TaxCategory taxCategory;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String documentPath;

}
