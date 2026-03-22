package es.upm.api.infrastructure.mongodb.entities;

import es.upm.api.domain.model.Expense;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document
public class ExpenseEntity {
    @Id
    private UUID id;
    private UUID engagementId;
    private BigDecimal amount;
    private LocalDate date;
    private String description;

    public ExpenseEntity(Expense expense) {
        BeanUtils.copyProperties(expense, this);
    }

    public Expense toExpense() {
        Expense expense = new Expense();
        BeanUtils.copyProperties(this, expense);
        return expense;
    }
}