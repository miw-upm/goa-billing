package es.upm.api.domain.ports.out.user;

import es.upm.api.domain.model.external.AccessLinkSnapshot;

public interface AccessLinkGateway {
    AccessLinkSnapshot consume(String scope, String urlId, String token);
}
