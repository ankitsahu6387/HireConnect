package com.hireconnect.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2SignupRoleFilter oAuth2SignupRoleFilter;

    public SecurityConfig(
            OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
            OAuth2SignupRoleFilter oAuth2SignupRoleFilter
    ) {
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.oAuth2SignupRoleFilter = oAuth2SignupRoleFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable()) //  disable CSRF for Postman
            .sessionManagement(session -> 
            session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
        )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**", "/oauth2/**", "/login/oauth2/**").permitAll() //  allow auth APIs
                .anyRequest().authenticated() // seccure
            )
            .oauth2Login(oauth2 -> oauth2
                .successHandler(oAuth2LoginSuccessHandler)
                .failureUrl("http://localhost:4200/login?oauthError=true")
            )
            .addFilterBefore(oAuth2SignupRoleFilter, OAuth2AuthorizationRequestRedirectFilter.class)
            .httpBasic(httpBasic -> httpBasic.disable()); //  disable basic auth popup

        return http.build();
    }
}
