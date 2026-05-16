package com.hireconnect.auth.controller;

import com.hireconnect.auth.config.JwtUtil;
import com.hireconnect.auth.dto.AuthRequest;
import com.hireconnect.auth.dto.AuthResponse;
import com.hireconnect.auth.dto.EmailOtpRequest;
import com.hireconnect.auth.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService service;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService service, JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/send-registration-otp")
    public String sendRegistrationOtp(@RequestBody EmailOtpRequest request) {
        return service.sendRegistrationOtp(request.getEmail());
    }

    @PostMapping("/verify-registration-otp")
    public String verifyRegistrationOtp(@RequestBody EmailOtpRequest request) {
        return service.verifyRegistrationOtp(request.getEmail(), request.getOtp());
    }

    @PostMapping("/send-password-reset-otp")
    public String sendPasswordResetOtp(@RequestBody EmailOtpRequest request) {
        return service.sendPasswordResetOtp(request.getEmail());
    }

    @PostMapping("/verify-password-reset-otp")
    public String verifyPasswordResetOtp(@RequestBody EmailOtpRequest request) {
        return service.verifyPasswordResetOtp(request.getEmail(), request.getOtp());
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestBody AuthRequest request) {
        return service.resetPassword(request);
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody AuthRequest request) {
        return service.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        return service.login(request);
    }

    @GetMapping("/validate")
    public String validate(@RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String resolvedToken = token;
        if ((resolvedToken == null || resolvedToken.isBlank()) && authorization != null) {
            resolvedToken = authorization.replaceFirst("(?i)^Bearer\\s+", "");
        }
        jwtUtil.validateToken(resolvedToken);
        return "Valid Token";
    }

    public String validate(String token) {
        return validate(token, null);
    }
}
