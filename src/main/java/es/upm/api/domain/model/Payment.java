package es.upm.api.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import es.upm.api.domain.model.external.EngagementSnapshot;
import es.upm.api.domain.model.external.UserSnapshot;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
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
public class Payment {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UUID id;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @NotNull
    @Past
    private LocalDate date;

    private EngagementSnapshot engagement;

    @NotNull
    private UserSnapshot user;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private PaymentMethod method;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean invoiced;
}
