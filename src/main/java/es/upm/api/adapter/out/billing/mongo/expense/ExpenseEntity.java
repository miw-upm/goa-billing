package es.upm.api.adapter.out.billing.mongo.expense;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.SupplierInfo;
import es.upm.api.domain.model.TaxCategory;
import es.upm.api.domain.model.external.EngagementSnapshot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document
public class ExpenseEntity {
    @Id
    private String id;
    private LocalDateTime recordedAt;
    private String engagementId;
    private String engagementIdCode64;
    private BigDecimal baseAmount;
    private Integer vatRate;
    private SupplierInfo supplier;
    private TaxCategory taxCategory;
    private LocalDate issueDate;
    private String description;
    private BigDecimal withholdingTax;
    private String documentPath;

    public ExpenseEntity(Expense expense) {
        BeanUtils.copyProperties(expense, this);
        this.id = expense.getId() == null ? null : expense.getId().toString();
        this.engagementId = expense.getEngagement() == null || expense.getEngagement().getId() == null
                ? null : expense.getEngagement().getId().toString();
        this.engagementIdCode64 = this.engagementId == null ? null : ExpenseEntity.encodeEngagementId(this.engagementId);
    }

    public Expense toDomain() {
        Expense expense = new Expense();
        BeanUtils.copyProperties(this, expense);
        expense.setId(this.id == null ? null : UUID.fromString(this.id));
        expense.setEngagement(this.engagementId == null ? null
                : EngagementSnapshot.builder().id(UUID.fromString(this.engagementId)).build());
        return expense;
    }

    public static String encodeEngagementId(String engagementId) {
        UUID uuid = UUID.fromString(engagementId);
        ByteBuffer byteBuffer = ByteBuffer.allocate(16);
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());
        return Base64.getEncoder().withoutPadding().encodeToString(byteBuffer.array());
    }
}
