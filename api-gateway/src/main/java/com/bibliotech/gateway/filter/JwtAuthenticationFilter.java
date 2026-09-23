package com.bibliotech.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final SecretKey key;

    // Endpoints that bypass authentication
    private static final List<String> OPEN_ENDPOINTS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/validate",
            "/actuator"
    );

    public JwtAuthenticationFilter(
            @Value("${bibliotech.jwt.secret:BibliotechSecretKeyForJwtAuthenticationVeryLongSecureKey2026!}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. Allow open endpoints
        if (isOpenEndpoint(path, request.getMethod())) {
            return chain.filter(exchange);
        }

        // 2. Validate Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Missing or invalid Authorization header for path: {}", path);
            return onError(exchange, "Authorization token is missing or malformed", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        // 3. Verify JWT token
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String username = claims.getSubject();
            String role = (String) claims.get("role");
            Object userId = claims.get("userId");
            Object studentId = claims.get("studentId");

            // Mutate request with extracted user information headers for downstream services
            ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate()
                    .header("X-User-Name", username != null ? username : "")
                    .header("X-User-Role", role != null ? role : "STUDENT")
                    .header("X-User-Id", userId != null ? userId.toString() : "")
                    .header("X-Student-Id", studentId != null ? studentId.toString() : "");

            return chain.filter(exchange.mutate().request(requestBuilder.build()).build());

        } catch (Exception e) {
            logger.warn("JWT token verification failed: {}", e.getMessage());
            return onError(exchange, "Invalid or expired JWT token: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean isOpenEndpoint(String path, HttpMethod method) {
        // Public auth endpoints
        for (String openEndpoint : OPEN_ENDPOINTS) {
            if (path.equals(openEndpoint)) {
                return true;
            }
        }
        // Allow GET requests to book catalog openly
        if (method == HttpMethod.GET && (path.equals("/api/books") || path.startsWith("/api/books/"))) {
            return true;
        }
        return false;
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format("{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}",
                java.time.LocalDateTime.now(), httpStatus.value(), httpStatus.getReasonPhrase(), err);

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100; // Run early in filter chain
    }
}
