package es.upm.api.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.Encryptors;

@Configuration
public class CryptoConfig {

    @Bean("bytesEncryptor")
    public BytesEncryptor bytesEncryptor(
            @Value("${app.crypto.salt}") String salt,
            @Value("${app.crypto.password}") String password
    ) {
        return Encryptors.standard(password, salt);
    }

    @Bean("legacyBytesEncryptor")
    @ConditionalOnProperty(prefix = "app.crypto", name = "previous-password")
    public BytesEncryptor legacyBytesEncryptor(
            @Value("${app.crypto.previous-salt}") String salt,
            @Value("${app.crypto.previous-password}") String password
    ) {
        return Encryptors.standard(password, salt);
    }
}
