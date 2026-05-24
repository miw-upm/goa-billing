package es.upm.api.domain.model;

import es.upm.api.domain.model.external.UserSnapshot;
import es.upm.miw.exception.BadRequestException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.Collectors;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingInfo {
    @NotNull
    private UUID userId;

    @NotBlank
    private String fullName;

    @NotBlank
    private String identity;

    @NotBlank
    private String fullAddress;

    private String concept;

    public void updateFrom(UserSnapshot userSnapshot) {
        this.fullName = Stream.of(userSnapshot.getFirstName(), userSnapshot.getFamilyName())
                .filter(part -> part != null && !part.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
        this.identity = userSnapshot.getIdentity() == null ? null : userSnapshot.getIdentity().trim();
        this.fullAddress = Stream.of(
                        userSnapshot.getAddress(),
                        userSnapshot.getCity(),
                        userSnapshot.getProvince(),
                        userSnapshot.getPostalCode() == null ? null : userSnapshot.getPostalCode().toString()
                )
                .filter(part -> part != null && !part.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(", "));

        if (identity == null || identity.isBlank() || fullAddress.isBlank()) {
            throw new BadRequestException("User data is incomplete to build billingInfo");
        }
    }
}
