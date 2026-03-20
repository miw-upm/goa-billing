package es.upm.api.infrastructure.mongodb.entities;

import es.upm.api.domain.model.Expense;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Document
public class ExpenseEntity {
    @Id
    private UUID id;
    private UUID engagementId;
    private BigDecimal amount;
    private LocalDate date;
    private String description;

    public ExpenseEntity() {
        // Empty for framework
    }

    public ExpenseEntity(Expense expense) {
        BeanUtils.copyProperties(expense, this);
    }

    public Expense toExpense() {
        Expense expense = new Expense();
        BeanUtils.copyProperties(this, expense);
        return expense;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}