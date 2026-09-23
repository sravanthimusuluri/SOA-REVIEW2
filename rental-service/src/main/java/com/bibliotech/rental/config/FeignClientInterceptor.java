package com.bibliotech.rental.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FeignClientInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
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
