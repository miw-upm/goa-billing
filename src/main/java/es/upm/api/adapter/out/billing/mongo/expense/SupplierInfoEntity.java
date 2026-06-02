package es.upm.api.adapter.out.billing.mongo.expense;

import es.upm.api.domain.model.SupplierInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierInfoEntity {
    private String name;
    private String identity;

    public SupplierInfoEntity(SupplierInfo supplierInfo) {
        BeanUtils.copyProperties(supplierInfo, this);
    }

    public SupplierInfo toDomain() {
        SupplierInfo supplierInfo = new SupplierInfo();
        BeanUtils.copyProperties(this, supplierInfo);
        return supplierInfo;
    }
}
