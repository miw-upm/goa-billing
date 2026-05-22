package es.upm.api.adapter.out.billing.mongo.invoice;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.Income;
import es.upm.api.domain.model.InvoiceOld;
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

    public InvoiceEntity(InvoiceOld invoiceOld) {
        BeanUtils.copyProperties(invoiceOld, this);
    }

    public InvoiceOld toDomain() {
        InvoiceOld invoiceOld = new InvoiceOld();
        BeanUtils.copyProperties(this, invoiceOld);
        return invoiceOld;
    }
}
