package com.hireconnect.auth.config;

import com.hireconnect.auth.entity.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class OAuth2SignupRoleFilter extends OncePerRequestFilter {

    public static final String SIGNUP_ROLE_SESSION_KEY = "OAUTH2_SIGNUP_ROLE";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (path != null && path.startsWith("/oauth2/authorization/")) {
            String role = request.getParameter("role");

            if (role != null && !role.isBlank()) {
                try {
                    Role.valueOf(role.trim().toUpperCase());
                    request.getSession(true).setAttribute(SIGNUP_ROLE_SESSION_KEY, role.trim().toUpperCase());
                } catch (IllegalArgumentException ignored) {
                    request.getSession(true).removeAttribute(SIGNUP_ROLE_SESSION_KEY);
                }
            } else {
                request.getSession(true).removeAttribute(SIGNUP_ROLE_SESSION_KEY);
            }
        }

        filterChain.doFilter(request, response);
    }
}
