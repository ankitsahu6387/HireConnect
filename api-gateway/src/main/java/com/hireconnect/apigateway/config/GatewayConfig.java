package com.hireconnect.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("AUTH-SERVICE", r -> r.path("/auth/**", "/oauth2/**", "/login/oauth2/**").uri("lb://auth-service"))
                .route("USER-SERVICE", r -> r.path("/users/**").uri("lb://user-service"))
                .route("JOB-SERVICE", r -> r.path("/jobs/**").uri("lb://job-service"))
                .route("APPLICATION-SERVICE", r -> r.path("/applications/**").uri("lb://application-service"))
                .route("INTERVIEW-SERVICE", r -> r.path("/interviews/**").uri("lb://interview-service"))
                .route("NOTIFICATION-SERVICE", r -> r.path("/notify/**", "/notifications/**").uri("lb://notification-service"))
                .route("SUBSCRIPTION-SERVICE", r -> r.path("/subscription/**", "/payment/**").uri("lb://subscription-service"))
                .route("ANALYTICS-SERVICE", r -> r.path("/analytics/**").uri("lb://analytics-service"))
                .build();
    }
}
