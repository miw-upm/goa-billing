package es.upm.api.adapter.out.billing.mongo.invoice;

import es.upm.api.domain.model.creation.InvoiceLegalProcedure;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceLegalProcedureEntity {
    private String title;
    private BigDecimal budget;
    private List<String> legalTasks;

    public InvoiceLegalProcedureEntity(InvoiceLegalProcedure legalProcedure) {
        BeanUtils.copyProperties(legalProcedure, this);
    }

    public InvoiceLegalProcedure toDomain() {
        InvoiceLegalProcedure legalProcedure = new InvoiceLegalProcedure();
        BeanUtils.copyProperties(this, legalProcedure);
        return legalProcedure;
    }
}
