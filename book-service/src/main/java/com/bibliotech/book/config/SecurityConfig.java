package com.bibliotech.book.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final String internalServiceToken;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            @Value("${bibliotech.internal-service.token:local-development-internal-token-change-me}") String internalServiceToken) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.internalServiceToken = internalServiceToken;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**", "/actuator/**").permitAll()
                .requestMatchers(request -> isInternalInventoryRequest(request)).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/books/*/decrement", "/api/books/*/increment").denyAll()
                // Availability check can be accessed by anyone
                .requestMatchers(HttpMethod.GET, "/api/books/*/availability").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/books/**").permitAll()
                // Book creation, update, delete requires LIBRARIAN or ADMIN
                .requestMatchers(HttpMethod.POST, "/api/books").hasAnyRole("LIBRARIAN", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/books/**").hasAnyRole("LIBRARIAN", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/books/**").hasAnyRole("LIBRARIAN", "ADMIN")
                .anyRequest().authenticated()
            )
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private boolean isInternalInventoryRequest(jakarta.servlet.http.HttpServletRequest request) {
        String path = request.getRequestURI();
        return "POST".equals(request.getMethod())
                && path.matches("/api/books/\\d+/(decrement|increment)")
                && internalServiceToken.equals(request.getHeader("X-Internal-Service-Token"));
    }
}
