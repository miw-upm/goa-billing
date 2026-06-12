package es.upm.api.adapter.out.billing.mongo.invoice;

import es.upm.api.domain.model.creation.LegalProcedure;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LegalProcedureEntity {
    private String title;
    private BigDecimal budget;
    private String budgetProposal;
    private List<String> legalTasks;

    public LegalProcedureEntity(LegalProcedure legalProcedure) {
        BeanUtils.copyProperties(legalProcedure, this);
    }

    public LegalProcedure toDomain() {
        LegalProcedure legalProcedure = new LegalProcedure();
        BeanUtils.copyProperties(this, legalProcedure);
        return legalProcedure;
    }
}
