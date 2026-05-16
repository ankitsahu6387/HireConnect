package com.hireconnect.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hireconnect.auth.config.JwtUtil;
import com.hireconnect.auth.dto.AuthRequest;
import com.hireconnect.auth.dto.AuthResponse;
import com.hireconnect.auth.dto.EmailOtpRequest;
import com.hireconnect.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService service;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthController controller;

    @Test
    void delegatesOtpEndpointsToService() {
        EmailOtpRequest request = new EmailOtpRequest("user@example.com", "123456");
        when(service.sendRegistrationOtp("user@example.com")).thenReturn("sent");
        when(service.verifyRegistrationOtp("user@example.com", "123456")).thenReturn("verified");
        when(service.sendPasswordResetOtp("user@example.com")).thenReturn("reset sent");
        when(service.verifyPasswordResetOtp("user@example.com", "123456")).thenReturn("reset verified");

        assertThat(controller.sendRegistrationOtp(request)).isEqualTo("sent");
        assertThat(controller.verifyRegistrationOtp(request)).isEqualTo("verified");
        assertThat(controller.sendPasswordResetOtp(request)).isEqualTo("reset sent");
        assertThat(controller.verifyPasswordResetOtp(request)).isEqualTo("reset verified");
    }

    @Test
    void delegatesAuthEndpointsToService() {
        AuthRequest request = new AuthRequest();
        AuthResponse response = new AuthResponse("token", "ok", "User", "user@example.com", "CANDIDATE", 1L);
        when(service.resetPassword(request)).thenReturn("reset");
        when(service.register(request)).thenReturn(response);
        when(service.login(request)).thenReturn(response);

        assertThat(controller.resetPassword(request)).isEqualTo("reset");
        assertThat(controller.register(request)).isSameAs(response);
        assertThat(controller.login(request)).isSameAs(response);
    }

    @Test
    void validateTokenDelegatesToJwtUtil() {
        assertThat(controller.validate("token")).isEqualTo("Valid Token");

        verify(jwtUtil).validateToken("token");
    }
}
