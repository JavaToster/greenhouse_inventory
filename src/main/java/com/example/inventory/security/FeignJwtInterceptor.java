package com.example.inventory.security;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FeignJwtInterceptor implements RequestInterceptor {

    private final static String BEARER_PREFIX = "Bearer ";

    @Override
    public void apply(RequestTemplate template) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getCredentials() instanceof String jwtToken) {
            log.debug("Inventory Feign interceptor: Injecting JWT Bearer token into outgoing request [{} {}]",
                    template.method(), template.url());
            template.header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + jwtToken);
        } else {
            log.warn("Inventory Feign interceptor: No valid JWT token found in SecurityContext. Request [{} {}] will be sent without Authorization header",
                    template.method(), template.url());
        }
    }
}