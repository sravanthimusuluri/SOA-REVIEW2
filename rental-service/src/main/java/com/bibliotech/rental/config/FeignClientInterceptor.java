package com.bibliotech.rental.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FeignClientInterceptor implements RequestInterceptor {

    private final String internalServiceToken;

    public FeignClientInterceptor(
            @Value("${bibliotech.internal-service.token:local-development-internal-token-change-me}") String internalServiceToken) {
        this.internalServiceToken = internalServiceToken;
    }

    @Override
    public void apply(RequestTemplate template) {
        template.header("X-Internal-Service-Token", internalServiceToken);

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && !authHeader.isBlank()) {
                template.header("Authorization", authHeader);
            }
            String userHeader = request.getHeader("X-User-Name");
            if (userHeader != null && !userHeader.isBlank()) {
                template.header("X-User-Name", userHeader);
            }
            String roleHeader = request.getHeader("X-User-Role");
            if (roleHeader != null && !roleHeader.isBlank()) {
                template.header("X-User-Role", roleHeader);
            }
        }
    }
}
