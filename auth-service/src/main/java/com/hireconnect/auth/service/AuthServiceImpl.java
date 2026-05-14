package com.hireconnect.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.hireconnect.auth.config.JwtUtil;
import com.hireconnect.auth.dto.AuthRequest;
import com.hireconnect.auth.dto.AuthResponse;
import com.hireconnect.auth.dto.EmailOtpRequest;
import com.hireconnect.auth.entity.Role;
import com.hireconnect.auth.entity.UserCredential;
import com.hireconnect.auth.exception.InvalidCredentialsException;
import com.hireconnect.auth.exception.ResourceNotFoundException;
import com.hireconnect.auth.repository.AuthRepository;
import com.hireconnect.auth.client.NotificationClient;
import com.hireconnect.auth.client.UserClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    private static final long VERIFIED_OTP_VALID_MINUTES = 10;
    private static final String REGISTRATION_OTP_KEY_PREFIX = "auth:otp:registration:";
    private static final String PASSWORD_RESET_OTP_KEY_PREFIX = "auth:otp:password-reset:";

    @Autowired
    private AuthRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserClient userClient;

    @Autowired
    private NotificationClient notificationClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    // PASSWORD VALIDATION
    private void validatePassword(String password) {
        if (password.length() < 6)
            throw new InvalidCredentialsException("Password must be at least 6 characters");

        if (!password.matches(".*[A-Z].*"))
            throw new InvalidCredentialsException("Password must contain uppercase letter");

        if (!password.matches(".*[0-9].*"))
            throw new InvalidCredentialsException("Password must contain number");
    }
    
    private void validateUsername(String name) {

        if (name == null || name.trim().length() < 2) {
            throw new InvalidCredentialsException("Username must be at least 2 characters");
        }

        if (!name.matches("^[a-zA-Z ]+$")) {
            throw new InvalidCredentialsException("Username should contain only letters");
        }
        
        if (name.contains("  "))
            throw new InvalidCredentialsException("Username cannot contain multiple space");
    }

    private void validateEmail(String email) {
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new InvalidCredentialsException("Invalid email format");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    @Override
    public String sendRegistrationOtp(String email) {
        String normalizedEmail = normalizeEmail(email);
        validateEmail(normalizedEmail);

        if (repository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new InvalidCredentialsException("User already exists");
        }

        try {
            notificationClient.sendRegistrationOtp(new EmailOtpRequest(normalizedEmail, null));
            return "Verification OTP sent successfully";
        } catch (Exception e) {
            throw new InvalidCredentialsException("Unable to send verification OTP");
        }
    }

    @Override
    public String verifyRegistrationOtp(String email, String otp) {
        String normalizedEmail = normalizeEmail(email);
        validateEmail(normalizedEmail);

        if (repository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new InvalidCredentialsException("User already exists");
        }

        if (otp == null || otp.isBlank()) {
            throw new InvalidCredentialsException("Email verification OTP is required");
        }

        try {
            notificationClient.verifyRegistrationOtp(new EmailOtpRequest(normalizedEmail, otp));
            saveVerifiedOtp(REGISTRATION_OTP_KEY_PREFIX, normalizedEmail, otp);
            return "Email verified successfully";
        } catch (Exception e) {
            throw new InvalidCredentialsException("Invalid or expired OTP");
        }
    }

    @Override
    public String sendPasswordResetOtp(String email) {
        String normalizedEmail = normalizeEmail(email);
        validateEmail(normalizedEmail);

        if (!repository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResourceNotFoundException("User not found");
        }

        try {
            notificationClient.sendPasswordResetOtp(new EmailOtpRequest(normalizedEmail, null));
            return "Password reset OTP sent successfully";
        } catch (Exception e) {
            throw new InvalidCredentialsException("Unable to send password reset OTP");
        }
    }

    @Override
    public String verifyPasswordResetOtp(String email, String otp) {
        String normalizedEmail = normalizeEmail(email);
        validateEmail(normalizedEmail);

        if (!repository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResourceNotFoundException("User not found");
        }

        if (otp == null || otp.isBlank()) {
            throw new InvalidCredentialsException("Password reset OTP is required");
        }

        try {
            notificationClient.verifyPasswordResetOtp(new EmailOtpRequest(normalizedEmail, otp));
            saveVerifiedOtp(PASSWORD_RESET_OTP_KEY_PREFIX, normalizedEmail, otp);
            return "Email verified successfully";
        } catch (Exception e) {
            throw new InvalidCredentialsException("Invalid or expired OTP");
        }
    }

    @Override
    public String resetPassword(AuthRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        validateEmail(normalizedEmail);
        validatePassword(request.getPassword());

        UserCredential user = repository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        consumeVerifiedOtp(
                PASSWORD_RESET_OTP_KEY_PREFIX,
                normalizedEmail,
                request.getOtp(),
                "Password reset OTP is required",
                () -> notificationClient.verifyPasswordResetOtp(new EmailOtpRequest(normalizedEmail, request.getOtp()))
        );

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        repository.save(user);

        return "Password reset successfully";
    }

    @Override
    public AuthResponse register(AuthRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        if (repository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new InvalidCredentialsException("User already exists");
        }

        validateUsername(request.getName());
        validateEmail(normalizedEmail);
        validatePassword(request.getPassword());
        consumeVerifiedRegistrationOtp(normalizedEmail, request.getOtp());

        Role role;
        try {
            role = Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidCredentialsException("Invalid role provided");
        }

        UserCredential user = new UserCredential(
                request.getName().trim(),
                normalizedEmail,
                passwordEncoder.encode(request.getPassword()),
                role,
                true
        );

        user = repository.save(user);

        // Notify user-service to create the profile
        try {
            Map<String, Object> userDto = new HashMap<>();
            userDto.put("name", user.getName());
            userDto.put("email", user.getEmail());
            userDto.put("role", user.getRole().name());
            userClient.createUser(user.getId(), userDto);
        } catch (Exception e) {
            // Log it, but we can still return success for auth or rollback. For now log and continue.
            System.err.println("Failed to create user profile in user-service: " + e.getMessage());
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return new AuthResponse(
                token,
                "Registered Successfully",
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getId()
        );
    }

    private void consumeVerifiedRegistrationOtp(String email, String otp) {
        consumeVerifiedOtp(
                REGISTRATION_OTP_KEY_PREFIX,
                email,
                otp,
                "Email verification OTP is required",
                () -> notificationClient.verifyRegistrationOtp(new EmailOtpRequest(email, otp))
        );
    }

    @Override
    public AuthResponse login(AuthRequest request) {

        UserCredential user = repository.findByEmailIgnoreCase(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid password");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return new AuthResponse(
                token,
                "Login Successful",
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getId()
        );
    }

    private void saveVerifiedOtp(String keyPrefix, String email, String otp) {
        redisTemplate.opsForValue().set(
                otpKey(keyPrefix, email),
                otp.trim(),
                Duration.ofMinutes(VERIFIED_OTP_VALID_MINUTES)
        );
    }

    private String otpKey(String keyPrefix, String email) {
        return keyPrefix + email;
    }

    private void consumeVerifiedOtp(
            String keyPrefix,
            String email,
            String otp,
            String requiredMessage,
            Runnable fallbackVerifier
    ) {
        if (otp == null || otp.isBlank()) {
            throw new InvalidCredentialsException(requiredMessage);
        }

        String redisKey = otpKey(keyPrefix, email);
        String verifiedOtp = redisTemplate.opsForValue().get(redisKey);
        if (verifiedOtp != null) {
            if (verifiedOtp.equals(otp.trim())) {
                redisTemplate.delete(redisKey);
                return;
            }
        }

        try {
            fallbackVerifier.run();
        } catch (Exception e) {
            throw new InvalidCredentialsException("Invalid or expired OTP");
        }
    }
}
