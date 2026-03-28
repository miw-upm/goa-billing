package es.upm.api.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseFindCriteria {
    private LocalDate date;

    public boolean all() {
        return date == null;
    }
}
