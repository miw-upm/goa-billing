package es.upm.api.domain.model.criteria;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFindCriteria {
    private Boolean invoiced;
    private String client;
    private LocalDate fromDate;
    private String engagementReference;

    public PaymentFindCriteria(Boolean invoiced, String client, LocalDate fromDate) {
        this.invoiced = invoiced;
        this.client = client;
        this.fromDate = fromDate;
    }

    public boolean all() {
        return this.invoiced == null && this.client == null && this.fromDate == null && this.engagementReference == null;
    }
}
