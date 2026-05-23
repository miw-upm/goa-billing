package es.upm.api.domain.model.criteria;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseFindCriteria {
    private UUID engagementId;
    private LocalDate fromDate;

    public boolean isEmpty() {
        return engagementId == null && fromDate == null;
    }
}
