package es.upm.api.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class Income {
    private UUID id;

    @NotNull
    private UUID engagementId;

    @NotNull
    private UUID userId;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
}