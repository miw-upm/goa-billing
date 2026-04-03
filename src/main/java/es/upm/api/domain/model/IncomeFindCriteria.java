package es.upm.api.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncomeFindCriteria {
    private UUID engagementId;
    private LocalDate date;

    public boolean isEmpty() {
        return this.engagementId == null && this.date == null;
    }
}