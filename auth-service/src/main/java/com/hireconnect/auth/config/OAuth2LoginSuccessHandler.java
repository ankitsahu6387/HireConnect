package com.hireconnect.auth.config;

import com.hireconnect.auth.client.UserClient;
import com.hireconnect.auth.entity.Role;
import com.hireconnect.auth.entity.UserCredential;
import com.hireconnect.auth.repository.AuthRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserClient userClient;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.oauth2.redirect-success-url:http://localhost:4200/oauth/callback}")
    private String redirectSuccessUrl;

    public OAuth2LoginSuccessHandler(
            AuthRepository repository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            UserClient userClient,
            OAuth2AuthorizedClientService authorizedClientService
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userClient = userClient;
        this.authorizedClientService = authorizedClientService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String email = principal.getAttribute("email");
        if (email == null || email.isBlank()) {
            email = fetchGithubPrimaryEmail(authentication);
        }

        if (email == null || email.isBlank()) {
            response.sendRedirect("http://localhost:4200/login?oauthError=email");
            return;
        }
        String oauthEmail = email.trim().toLowerCase();

        String name = principal.getAttribute("name");
        if (name == null || name.isBlank()) {
            name = oauthEmail.substring(0, oauthEmail.indexOf("@"));
        }
        String oauthName = name;

        UserCredential user = repository.findByEmailIgnoreCase(oauthEmail).orElse(null);
        if (user == null) {
            Role signupRole = getSignupRole(request);
            if (signupRole == null) {
                response.sendRedirect(redirectSuccessUrl
                        + "?oauthSignup=true"
                        + "&provider=" + encode(resolveProvider(authentication))
                        + "&name=" + encode(oauthName)
                        + "&email=" + encode(oauthEmail));
                return;
            }

            user = createOAuthUser(oauthName, oauthEmail, signupRole);
        } else {
            if (oauthName != null && !oauthName.isBlank() && !oauthName.equals(user.getName())) {
                user.setName(oauthName);
                user = repository.save(user);
            }
            ensureUserProfile(user);
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(OAuth2SignupRoleFilter.SIGNUP_ROLE_SESSION_KEY);
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        String redirectUrl = redirectSuccessUrl
                + "?token=" + encode(token)
                + "&name=" + encode(user.getName())
                + "&email=" + encode(user.getEmail())
                + "&role=" + encode(user.getRole().name())
                + "&userId=" + user.getId();

        response.sendRedirect(redirectUrl);
    }

    private UserCredential createOAuthUser(String name, String email, Role role) {
        UserCredential user = new UserCredential(
                name,
                email,
                passwordEncoder.encode("OAUTH2_LOGIN_ONLY"),
                role,
                true
        );

        user = repository.save(user);

        try {
            Map<String, Object> userDto = new HashMap<>();
            userDto.put("name", user.getName());
            userDto.put("email", user.getEmail());
            userDto.put("role", user.getRole().name());
            userClient.createUser(user.getId(), userDto);
        } catch (Exception e) {
            System.err.println("Failed to create OAuth user profile in user-service: " + e.getMessage());
        }

        return user;
    }

    private void ensureUserProfile(UserCredential user) {
        try {
            userClient.getUser(user.getId());
        } catch (Exception e) {
            try {
                Map<String, Object> userDto = new HashMap<>();
                userDto.put("name", user.getName());
                userDto.put("email", user.getEmail());
                userDto.put("role", user.getRole().name());
                userClient.createUser(user.getId(), userDto);
            } catch (Exception createException) {
                System.err.println("Failed to ensure OAuth user profile in user-service: " + createException.getMessage());
            }
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private Role getSignupRole(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object role = session == null
                ? null
                : session.getAttribute(OAuth2SignupRoleFilter.SIGNUP_ROLE_SESSION_KEY);

        if (role == null) {
            return null;
        }

        try {
            return Role.valueOf(String.valueOf(role));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String resolveProvider(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken token) {
            return token.getAuthorizedClientRegistrationId();
        }

        return "google";
    }

    private String fetchGithubPrimaryEmail(Authentication authentication) {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                "github",
                authentication.getName()
        );

        if (client == null || client.getAccessToken() == null) {
            return null;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(client.getAccessToken().getTokenValue());
        headers.set(HttpHeaders.ACCEPT, "application/vnd.github+json");

        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    "https://api.github.com/user/emails",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {}
            );

            List<Map<String, Object>> emails = response.getBody();
            if (emails == null) {
                return null;
            }

            return emails.stream()
                    .filter(email -> Boolean.TRUE.equals(email.get("primary")))
                    .filter(email -> Boolean.TRUE.equals(email.get("verified")))
                    .map(email -> (String) email.get("email"))
                    .findFirst()
                    .orElseGet(() -> emails.stream()
                            .filter(email -> Boolean.TRUE.equals(email.get("verified")))
                            .map(email -> (String) email.get("email"))
                            .findFirst()
                            .orElse(null));
        } catch (RestClientException e) {
            System.err.println("Failed to fetch GitHub email address: " + e.getMessage());
            return null;
        }
    }
}
