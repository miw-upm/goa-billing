package es.upm.api.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import es.upm.api.domain.model.external.EngagementSnapshot;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Expense {
    public static final int CURRENT = 100;
    private static final int SCALE = 6;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UUID id;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime recordedAt;

    private String series;

    private Integer number;

    private EngagementSnapshot engagement;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @NotNull
    @PastOrPresent
    private LocalDate issueDate;

    @NotNull
    @Positive
    private BigDecimal baseAmount;

    @NotNull
    @Positive
    private Integer vatRate;

    @Valid
    @NotNull
    private SupplierInfo supplier;

    @NotNull
    private TaxCategory taxCategory;

    @NotNull
    private Integer depreciationRate;

    private String description;

    @PositiveOrZero
    private BigDecimal withholdingTax = BigDecimal.ZERO;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String documentPath;

    public boolean isCapital() {
        return depreciationRate != CURRENT;
    }

    public BigDecimal vatFactor() {
        return BigDecimal.valueOf(this.vatRate).divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal vatAmount() {
        return this.baseAmount.multiply(this.vatFactor());
    }

}
