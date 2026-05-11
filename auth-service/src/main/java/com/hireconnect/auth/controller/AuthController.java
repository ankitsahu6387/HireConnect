package com.hireconnect.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.hireconnect.auth.dto.AuthRequest;
import com.hireconnect.auth.dto.AuthResponse;
import com.hireconnect.auth.dto.EmailOtpRequest;
import com.hireconnect.auth.service.AuthService;
import com.hireconnect.auth.config.JwtUtil;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService service;

    @Autowired
    private JwtUtil jwtUtil;

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
    public String validate(@RequestParam String token) {
        jwtUtil.validateToken(token);
        return "Valid Token";
    }
}
