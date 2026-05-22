package es.upm.api.domain.model.criteria;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFindCriteria {
    private String client;
    private LocalDate fromDate;

    public boolean all() {
        return this.client == null && this.fromDate == null;
    }
}
