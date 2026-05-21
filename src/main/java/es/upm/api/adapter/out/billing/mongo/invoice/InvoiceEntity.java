package es.upm.api.adapter.out.billing.mongo.invoice;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.Income;
import es.upm.api.domain.model.Invoice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document
public class InvoiceEntity {
    @Id
    private UUID id;
    private UUID engagementId;
    private LocalDate date;
    private List<Expense> expenses;
    private List<Income> incomes;

    public InvoiceEntity(Invoice invoice) {
        BeanUtils.copyProperties(invoice, this);
    }

    public Invoice toDomain() {
        Invoice invoice = new Invoice();
        BeanUtils.copyProperties(this, invoice);
        return invoice;
    }
}
