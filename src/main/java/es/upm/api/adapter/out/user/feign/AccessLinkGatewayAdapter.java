package es.upm.api.adapter.out.user.feign;

import es.upm.api.domain.model.external.AccessLinkSnapshot;
import es.upm.api.domain.ports.out.user.AccessLinkGateway;
import es.upm.miw.exception.BadGatewayException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccessLinkGatewayAdapter implements AccessLinkGateway {
    private final GoaUserClient goaUserClient;

    @Override
    public AccessLinkSnapshot consume(String scope, String urlId, String token) {
        try {
            return this.goaUserClient.consumeAccessLinkToken(scope, urlId, token);
        } catch (Exception exception) {
            throw new BadGatewayException(exception.getMessage() + " on consume", exception.getCause());
        }
    }
}
