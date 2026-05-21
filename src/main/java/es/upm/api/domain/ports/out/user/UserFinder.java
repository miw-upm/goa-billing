package es.upm.api.domain.ports.out.user;

import es.upm.api.domain.model.external.UserSnapshot;

import java.util.List;
import java.util.UUID;

public interface UserFinder {
    UserSnapshot readById(UUID id);

    UserSnapshot readByMobile(String mobile);

    List<UserSnapshot> find(String customer);

    UserSnapshot readByUrlIdWithToken(String scope, String urlId, String token);

}
