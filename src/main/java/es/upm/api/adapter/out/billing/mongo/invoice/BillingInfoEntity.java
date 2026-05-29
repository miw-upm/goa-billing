package es.upm.api.adapter.out.billing.mongo.invoice;

import es.upm.api.domain.model.BillingInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingInfoEntity {
    private String userId;
    private String fullName;
    private String identity;
    private String fullAddress;

    public BillingInfoEntity(BillingInfo billingInfo) {
        BeanUtils.copyProperties(billingInfo, this);
        this.userId = billingInfo.getUserId() == null ? null : billingInfo.getUserId().toString();
    }

    public BillingInfo toDomain() {
        BillingInfo billingInfo = new BillingInfo();
        BeanUtils.copyProperties(this, billingInfo);
        billingInfo.setUserId(this.userId == null ? null : UUID.fromString(this.userId));
        return billingInfo;
    }
}
