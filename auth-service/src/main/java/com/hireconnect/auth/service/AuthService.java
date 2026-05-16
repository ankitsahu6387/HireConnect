package com.hireconnect.auth.service;

import com.hireconnect.auth.dto.AuthRequest;
import com.hireconnect.auth.dto.AuthResponse;

public interface AuthService {

    String sendRegistrationOtp(String email);

    String verifyRegistrationOtp(String email, String otp);

    String sendPasswordResetOtp(String email);

    String verifyPasswordResetOtp(String email, String otp);

    String resetPassword(AuthRequest request);

    AuthResponse register(AuthRequest request);

    AuthResponse login(AuthRequest request);
}
