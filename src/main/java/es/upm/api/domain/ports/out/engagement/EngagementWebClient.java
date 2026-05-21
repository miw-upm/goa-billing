package es.upm.api.domain.ports.out.engagement;

import es.upm.api.configurations.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = EngagementWebClient.GOA_ENGAGEMENT, configuration = FeignConfig.class)
public interface EngagementWebClient {
    String GOA_ENGAGEMENT = "goa-engagement";
    String ENGAGEMENT_LETTERS = "/engagement-letters";
    String ID_ID = "/{id}";

    @GetMapping(ENGAGEMENT_LETTERS + ID_ID)
    Object readById(@PathVariable UUID id);
}