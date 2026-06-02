package es.upm.api.adapter.out.user.feign;

import es.upm.api.domain.model.external.UserSnapshot;
import es.upm.api.domain.ports.out.user.UserFinder;
import es.upm.miw.exception.BadGatewayException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class UserFinderAdapter implements UserFinder {
    private final GoaUserClient goaUserClient;

    @Override
    public UserSnapshot readById(UUID id) {
        return this.call(() -> this.goaUserClient.readUserById(id), " on read user by id");
    }

    @Override
    public UserSnapshot readByMobile(String mobile) {
        return this.call(() -> this.goaUserClient.readUserByMobile(mobile), " on read user by mobile");
    }

    @Override
    public List<UserSnapshot> find(String customer) {
        return this.call(() -> this.goaUserClient.findUser(customer), " on find users");
    }

    @Override
    public UserSnapshot readByUrlIdWithToken(String scope, String urlId, String token) {
        return this.call(() -> this.goaUserClient.readUserByUrlIdWithToken(scope, urlId, token), " on read user by urlId with token");
    }

    private <T> T call(Supplier<T> supplier, String operation) {
        try {
            return supplier.get();
        } catch (Exception exception) {
            throw new BadGatewayException(exception.getMessage() + operation, exception.getCause());
        }
    }
}
