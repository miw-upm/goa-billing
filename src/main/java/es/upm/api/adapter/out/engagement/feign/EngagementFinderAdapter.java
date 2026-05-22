package es.upm.api.adapter.out.engagement.feign;

import es.upm.api.domain.model.external.EngagementSnapshot;
import es.upm.api.domain.ports.out.engagement.EngagementFinder;
import es.upm.miw.exception.BadGatewayException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EngagementFinderAdapter implements EngagementFinder {
    private final EngagementWebClient engagementWebClient;

    @Override
    public EngagementSnapshot read(UUID id) {
        try {
            return this.engagementWebClient.read(id);
        } catch (Exception exception) {
            throw new BadGatewayException(exception.getMessage() + " on read engagement by id", exception.getCause());
        }
    }
}
