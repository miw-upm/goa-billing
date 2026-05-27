package es.upm.api.domain.model.external;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EngagementSnapshot {
    private UUID id;
    private UserSnapshot owner;
    private List<LegalProcedureSnapshot> legalProcedures;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastUpdatedDate;
    private LocalDate closingDate;
    private String reference;
    private List<BigDecimal> discounts;
}
