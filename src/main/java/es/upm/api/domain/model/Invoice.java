package es.upm.api.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import es.upm.api.domain.model.external.EngagementSnapshot;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UUID id;

    @NotNull
    private BillingInfo billingInfo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDate emissionDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @PastOrPresent
    private LocalDate operationDate;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String series;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer number;

    @NotNull
    @Positive
    private BigDecimal baseAmount;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal vatRate;

    private EngagementSnapshot engagement;
    private List<Payment> payments;
    private List<BigDecimal> discounts;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String pdfPath;

    private Rectification rectification;
}
