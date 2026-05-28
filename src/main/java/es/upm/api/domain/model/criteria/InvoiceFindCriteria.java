package es.upm.api.domain.model.criteria;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceFindCriteria {
    private String client;
    private LocalDate fromDate;
    private String engagementReference;

    public InvoiceFindCriteria(String client, LocalDate fromDate) {
        this.client = client;
        this.fromDate = fromDate;
    }

    public boolean isEmpty() {
        return this.client == null && this.fromDate == null && this.engagementReference == null;
    }
}
