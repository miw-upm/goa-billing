package es.upm.api.infrastructure.resources.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

public class IncomeUpdateRequest {
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

    public UUID getEngagementId() {
        return engagementId;
    }

    public void setEngagementId(UUID engagementId) {
        this.engagementId = engagementId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
