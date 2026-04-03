package es.upm.api.domain.model;

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
    private LocalDate date;

    public boolean all() {
        return engagementId == null && date == null;
    }
}
