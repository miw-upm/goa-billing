package es.upm.api.domain.ports.out.engagement;

import es.upm.api.domain.model.external.EngagementSnapshot;

import java.util.UUID;

public interface EngagementGateway {
    EngagementSnapshot read(UUID id);
    void close(UUID id);
}