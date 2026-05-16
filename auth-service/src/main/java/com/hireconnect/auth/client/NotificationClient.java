package com.hireconnect.auth.client;

import com.hireconnect.auth.dto.EmailOtpRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    @PostMapping("/notify/send-registration-otp")
    Object sendRegistrationOtp(@RequestBody EmailOtpRequest request);

    @PostMapping("/notify/verify-registration-otp")
    Object verifyRegistrationOtp(@RequestBody EmailOtpRequest request);

    @PostMapping("/notify/send-password-reset-otp")
    Object sendPasswordResetOtp(@RequestBody EmailOtpRequest request);

    @PostMapping("/notify/verify-password-reset-otp")
    Object verifyPasswordResetOtp(@RequestBody EmailOtpRequest request);
}
