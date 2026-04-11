package es.upm.api.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceBreakdown {
    private BigDecimal taxableBase;
    private BigDecimal vatAmount;
    private BigDecimal totalAmount;
    private List<BreakdownItem> incomes;
    private List<BreakdownItem> expenses;
}


