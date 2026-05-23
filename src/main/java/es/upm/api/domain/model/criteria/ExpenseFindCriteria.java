package es.upm.api.domain.model.criteria;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseFindCriteria {
    private String category;
    private String supplier;
    private LocalDate fromDate;

    public boolean isEmpty() {
        return this.category == null && this.supplier == null && this.fromDate == null;
    }
}
