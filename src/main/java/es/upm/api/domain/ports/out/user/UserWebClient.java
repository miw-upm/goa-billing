package es.upm.api.domain.ports.out.user;

import es.upm.api.configurations.FeignConfig;
import es.upm.api.domain.model.external.UserSnapshot;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = UserWebClient.GOA_USER, configuration = FeignConfig.class)
public interface UserWebClient {
    String GOA_USER = "goa-user";
    String USERS = "/users";
    String ID_ID = "/{id}";
    String MOBILE_ID = "/{mobile}";

    @GetMapping(USERS + ID_ID)
    UserSnapshot readUserById(@PathVariable UUID id);

    @GetMapping(USERS + MOBILE_ID)
    UserSnapshot readUserByMobile(@PathVariable String mobile);

    @GetMapping(USERS)
    List<UserSnapshot> findNullSafe(@RequestParam(required = false) String attribute);

}
