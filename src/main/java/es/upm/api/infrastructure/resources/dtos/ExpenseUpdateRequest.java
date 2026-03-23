package es.upm.api.infrastructure.resources.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public class ExpenseUpdateRequest {

    @NotNull
    private UUID engagementId;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    private String description;

    public UUID getEngagementId() {
        return engagementId;
    }

    public void setEngagementId(UUID engagementId) {
        this.engagementId = engagementId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
