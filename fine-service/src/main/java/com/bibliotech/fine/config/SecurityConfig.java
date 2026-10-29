package com.bibliotech.fine.config;

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
                .requestMatchers(request -> isInternalFineRequest(request)).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/fines/calculate").denyAll()
                .requestMatchers("/api/fines/**").authenticated()
                .anyRequest().authenticated()
            )
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private boolean isInternalFineRequest(jakarta.servlet.http.HttpServletRequest request) {
        return "POST".equals(request.getMethod())
                && "/api/fines/calculate".equals(request.getRequestURI())
                && internalServiceToken.equals(request.getHeader("X-Internal-Service-Token"));
    }
}
