package com.example.inventory.security;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;


@Component
public class FeignJwtInterceptor implements RequestInterceptor {

    private final static String BEARER_PREFIX = "Bearer ";

    @Override
    public void apply(RequestTemplate template) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication != null && authentication.getCredentials() instanceof String jwtToken){
            template.header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX+jwtToken);
        }
    }
}
