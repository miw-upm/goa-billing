package es.upm.api.domain.model.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSnapshot {
    public static final String SIN_DEFINIR = "(___SIN DEFINIR___)";
    private UUID id;
    private String firstName;
    private String familyName;
    private String identity;
    private String address;
    private String city;
    private String province;
    private Integer postalCode;


    public String toFullName() {
        return firstName + " " + valueOrUndefined(familyName);
    }

    public boolean isComplete() {
        return this.id != null
                && this.firstName != null
                && this.familyName != null
                && this.identity != null
                && this.address != null
                && this.city != null
                && this.province != null
                && this.postalCode != null;
    }

    private String valueOrUndefined(String value) {
        return value != null ? value : SIN_DEFINIR;
    }
}
