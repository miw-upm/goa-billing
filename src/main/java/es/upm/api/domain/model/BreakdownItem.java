package es.upm.api.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BreakdownItem {
    private UUID id;
    private BigDecimal amountWithVat;
    private BigDecimal taxableBase;
    private BigDecimal vatAmount;
}

