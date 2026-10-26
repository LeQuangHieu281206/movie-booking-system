/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.gateway.security;

import com.ttcsn.gateway.config.GatewaySecurityProperties;

import io.jsonwebtoken.JwtException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
/**
 *
 * @author quanghieu
 */
@Component
public class GatewayJwtFilter implements GlobalFilter, Ordered {

    static final String USER_ID_HEADER = "X-User-Id";
    static final String USER_ROLE_HEADER = "X-User-Role";
    static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String METHOD_OVERRIDE_HEADER = "X-HTTP-Method-Override";
    private static final String METHOD_OVERRIDE_HEADER_ALIAS = "X-Method-Override";
    private static final byte[] UNAUTHORIZED_BODY =
            "{\"message\":\"Bạn cần đăng nhập để tiếp tục\"}".getBytes(StandardCharsets.UTF_8);
    private static final byte[] FORBIDDEN_BODY =
            "{\"message\":\"Bạn không có quyền thực hiện thao tác này\"}".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NOT_FOUND_BODY =
            "{\"message\":\"Không tìm thấy tài nguyên\"}".getBytes(StandardCharsets.UTF_8);

    private final JwtVerifier jwtVerifier;
    private final GatewaySecurityProperties properties;

    public GatewayJwtFilter(JwtVerifier jwtVerifier, GatewaySecurityProperties properties) {
        this.jwtVerifier = jwtVerifier;
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (request.getHeaders().containsKey(METHOD_OVERRIDE_HEADER)
                || request.getHeaders().containsKey(METHOD_OVERRIDE_HEADER_ALIAS)) {
            return writeError(exchange, HttpStatus.FORBIDDEN, FORBIDDEN_BODY);
        }

        GatewayAuthorizationRules.Access access = GatewayAuthorizationRules.requiredAccess(request);

        if (access == GatewayAuthorizationRules.Access.INVALID) {
            return writeError(exchange, HttpStatus.NOT_FOUND, NOT_FOUND_BODY);
        }

        if (access == GatewayAuthorizationRules.Access.PUBLIC) {
            return forward(exchange, chain, null);
        }

        String token = bearerToken(request);
        if (token == null) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, UNAUTHORIZED_BODY);
        }

        JwtVerifier.VerifiedUser user;
        try {
            user = jwtVerifier.verify(token);
        } catch (JwtException | IllegalArgumentException ex) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, UNAUTHORIZED_BODY);
        }

        if (!GatewayAuthorizationRules.allows(access, user.role())) {
            return writeError(exchange, HttpStatus.FORBIDDEN, FORBIDDEN_BODY);
        }

        return forward(exchange, chain, user);
    }

    private Mono<Void> forward(ServerWebExchange exchange, GatewayFilterChain chain,
                               JwtVerifier.VerifiedUser user) {
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(USER_ROLE_HEADER);
                    headers.remove(INTERNAL_TOKEN_HEADER);

                    if (user != null) {
                        headers.set(USER_ID_HEADER, String.valueOf(user.userId()));
                        headers.set(USER_ROLE_HEADER, user.role());
                    }
                    headers.set(INTERNAL_TOKEN_HEADER, properties.internalToken());
                })
                .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private static String bearerToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private static Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, byte[] body) {
        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}