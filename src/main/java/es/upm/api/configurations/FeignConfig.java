package es.upm.api.configurations;

import feign.RequestInterceptor;
import feign.form.FormData;
import org.springframework.cloud.openfeign.support.JsonFormWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.multipart.MultipartFile;

@Configuration
public class FeignConfig {

    private final TokenManager tokenManager;

    public FeignConfig(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                String tokenValue = jwtAuth.getToken().getTokenValue();
                template.header("Authorization", "Bearer " + tokenValue);
            } else {
                template.header("Authorization", "Bearer " + tokenManager.getToken());
            }
        };
    }

    @Bean
    public JsonFormWriter jsonFormWriter() {
        return new JsonFormWriter() {
            @Override
            public boolean isApplicable(Object value) {
                return value != null
                        && super.isApplicable(value)
                        && !(value instanceof FormData)
                        && !(value instanceof byte[])
                        && !(value instanceof MultipartFile)
                        && value.getClass().getPackageName().startsWith("es.upm");
            }
        };
    }
}
