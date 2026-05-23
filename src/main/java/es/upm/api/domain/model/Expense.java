package es.upm.api.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import es.upm.api.domain.model.external.EngagementSnapshot;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Expense {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UUID id;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime recordedAt;

    private EngagementSnapshot engagement;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @NotNull
    @Past
    private LocalDate issueDate;

    @NotNull
    @Positive
    private BigDecimal baseAmount;

    @NotNull
    @Positive
    private Integer vatRate;

    @NotBlank
    private String supplier;

    @NotBlank
    private String supplierIdentity;

    @NotNull
    private TaxCategory taxCategory;

    private String description;

    @Positive
    private BigDecimal withholdingTax = BigDecimal.ZERO;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String documentPath;

}
