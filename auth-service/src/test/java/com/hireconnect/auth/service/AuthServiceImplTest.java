package com.hireconnect.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.hireconnect.auth.client.NotificationClient;
import com.hireconnect.auth.client.UserClient;
import com.hireconnect.auth.config.JwtUtil;
import com.hireconnect.auth.dto.AuthRequest;
import com.hireconnect.auth.dto.AuthResponse;
import com.hireconnect.auth.dto.EmailOtpRequest;
import com.hireconnect.auth.entity.Role;
import com.hireconnect.auth.entity.UserCredential;
import com.hireconnect.auth.exception.InvalidCredentialsException;
import com.hireconnect.auth.exception.ResourceNotFoundException;
import com.hireconnect.auth.repository.AuthRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserClient userClient;

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private AuthServiceImpl service;

    @Test
    void sendRegistrationOtpNormalizesEmailAndDelegates() {
        when(repository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);

        String result = service.sendRegistrationOtp(" USER@Example.COM ");

        assertThat(result).isEqualTo("Verification OTP sent successfully");
        verify(notificationClient).sendRegistrationOtp(any(EmailOtpRequest.class));
    }

    @Test
    void sendRegistrationOtpRejectsExistingUser() {
        when(repository.existsByEmailIgnoreCase("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.sendRegistrationOtp("user@example.com"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void verifyRegistrationOtpStoresVerifiedOtpForRegistration() {
        when(repository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);

        String result = service.verifyRegistrationOtp("user@example.com", "123456");

        assertThat(result).isEqualTo("Email verified successfully");
        verify(notificationClient).verifyRegistrationOtp(any(EmailOtpRequest.class));
    }

    @Test
    void registerConsumesVerifiedOtpAndReturnsToken() {
        AuthRequest request = registerRequest();
        when(repository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("encoded");
        when(repository.save(any(UserCredential.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtUtil.generateToken("user@example.com", "CANDIDATE")).thenReturn("token");

        service.verifyRegistrationOtp("user@example.com", "123456");
        AuthResponse response = service.register(request);

        assertThat(response.getToken()).isEqualTo("token");
        assertThat(response.getEmail()).isEqualTo("user@example.com");
        verify(userClient).createUser(any(), any());
    }

    @Test
    void registerRejectsWeakPassword() {
        AuthRequest request = registerRequest();
        request.setPassword("weak");

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("at least 6");
    }

    @Test
    void registerFallsBackToNotificationVerificationWhenOtpWasNotPreVerified() {
        AuthRequest request = registerRequest();
        when(repository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("encoded");
        when(repository.save(any(UserCredential.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtUtil.generateToken("user@example.com", "CANDIDATE")).thenReturn("token");

        service.register(request);

        verify(notificationClient).verifyRegistrationOtp(any(EmailOtpRequest.class));
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        UserCredential user = new UserCredential("User", "user@example.com", "encoded", Role.EMPLOYER, true);
        when(repository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password1", "encoded")).thenReturn(true);
        when(jwtUtil.generateToken("user@example.com", "EMPLOYER")).thenReturn("token");

        AuthResponse response = service.login(loginRequest());

        assertThat(response.getToken()).isEqualTo("token");
        assertThat(response.getRole()).isEqualTo("EMPLOYER");
    }

    @Test
    void loginRejectsWrongPassword() {
        UserCredential user = new UserCredential("User", "user@example.com", "encoded", Role.EMPLOYER, true);
        when(repository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password1", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> service.login(loginRequest()))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid password");
    }

    @Test
    void resetPasswordRequiresExistingUserAndVerifiedOtp() {
        AuthRequest request = loginRequest();
        request.setOtp("123456");
        UserCredential user = new UserCredential("User", "user@example.com", "old", Role.CANDIDATE, true);
        when(repository.existsByEmailIgnoreCase("user@example.com")).thenReturn(true);
        when(repository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("Password1")).thenReturn("encoded");

        service.verifyPasswordResetOtp("user@example.com", "123456");
        String result = service.resetPassword(request);

        assertThat(result).isEqualTo("Password reset successfully");
        assertThat(user.getPassword()).isEqualTo("encoded");
        verify(repository).save(user);
    }

    @Test
    void sendPasswordResetOtpRejectsUnknownUser() {
        when(repository.existsByEmailIgnoreCase("missing@example.com")).thenReturn(false);

        assertThatThrownBy(() -> service.sendPasswordResetOtp("missing@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(notificationClient, never()).sendPasswordResetOtp(any());
    }

    private AuthRequest registerRequest() {
        AuthRequest request = loginRequest();
        request.setName("User Name");
        request.setRole("candidate");
        request.setOtp("123456");
        return request;
    }

    private AuthRequest loginRequest() {
        AuthRequest request = new AuthRequest();
        request.setEmail(" USER@Example.COM ");
        request.setPassword("Password1");
        return request;
    }
}
