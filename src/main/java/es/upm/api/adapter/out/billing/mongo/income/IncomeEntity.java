package es.upm.api.adapter.out.billing.mongo.income;

import es.upm.api.domain.model.Income;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document
public class IncomeEntity {
    @Id
    private UUID id;
    private UUID engagementId;
    private UUID userId;
    private BigDecimal amount;
    private LocalDate date;

    public IncomeEntity(Income income) {
        BeanUtils.copyProperties(income, this);
    }

    public Income toDomain() {
        Income income = new Income();
        BeanUtils.copyProperties(this, income);
        return income;
    }
}