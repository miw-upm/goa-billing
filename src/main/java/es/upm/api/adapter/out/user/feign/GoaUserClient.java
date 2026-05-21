package es.upm.api.adapter.out.user.feign;

import es.upm.api.configurations.FeignConfig;
import es.upm.api.domain.model.external.AccessLinkSnapshot;
import es.upm.api.domain.model.external.UserSnapshot;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(name = GoaUserClient.GOA_USER, configuration = FeignConfig.class)
public interface GoaUserClient {
    String GOA_USER = "goa-user";
    String USERS = "/users";
    String ACCESS_LINKS = "/access-links";
    String ID_ID = "/{id}";
    String MOBILE_ID = "/{mobile}";
    String SCOPE_ID = "/{scope}";
    String TOKEN_ID = "/{token}";
    String CONSUME = "/consume";

    @GetMapping(USERS + ID_ID)
    UserSnapshot readUserById(@PathVariable UUID id);

    @GetMapping(USERS + MOBILE_ID)
    UserSnapshot readUserByMobile(@PathVariable String mobile);

    @GetMapping(USERS)
    List<UserSnapshot> findUser(@RequestParam(required = false) String customer);

    @GetMapping(USERS + SCOPE_ID + ID_ID + TOKEN_ID)
    UserSnapshot readUserByUrlIdWithToken(@PathVariable String scope, @PathVariable String id, @PathVariable String token);


    @PostMapping(ACCESS_LINKS + SCOPE_ID + ID_ID + CONSUME)
    AccessLinkSnapshot consumeAccessLinkToken(@PathVariable String scope, @PathVariable String id, @RequestBody String token);
}
