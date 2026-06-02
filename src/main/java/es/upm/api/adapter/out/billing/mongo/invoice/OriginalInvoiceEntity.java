package es.upm.api.adapter.out.billing.mongo.invoice;

import es.upm.api.domain.model.OriginalInvoice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.time.LocalDate;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OriginalInvoiceEntity {
    private String series;
    private Integer number;
    private LocalDate emissionDate;
    private String reason;

    public OriginalInvoiceEntity(OriginalInvoice originalInvoice) {
        BeanUtils.copyProperties(originalInvoice, this);
    }

    public OriginalInvoice toDomain() {
        OriginalInvoice originalInvoice = new OriginalInvoice();
        BeanUtils.copyProperties(this, originalInvoice);
        return originalInvoice;
    }
}
