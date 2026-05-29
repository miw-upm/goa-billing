package es.upm.api.adapter.out.billing.mongo.invoice;

import es.upm.api.domain.model.InvoicedExpense;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoicedExpenseEntity {
    private String expenseId;
    private LocalDate issueDate;
    private String description;
    private BigDecimal baseAmount;
    private BigDecimal vatAmount;

    public InvoicedExpenseEntity(InvoicedExpense invoicedExpense) {
        BeanUtils.copyProperties(invoicedExpense, this);
        this.expenseId = invoicedExpense.expenseId() == null ? null : invoicedExpense.expenseId().toString();
    }

    public InvoicedExpense toDomain() {
        return new InvoicedExpense(
                this.getExpenseId() == null ? null : UUID.fromString(this.getExpenseId()),
                this.getIssueDate(),
                this.getDescription(),
                this.getBaseAmount(),
                this.getVatAmount()
        );
    }
}
