package com.hireconnect.notificationservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hireconnect.notificationservice.dto.ApiResponse;
import com.hireconnect.notificationservice.dto.EmailOtpRequest;
import com.hireconnect.notificationservice.dto.NewJobAlertRequest;
import com.hireconnect.notificationservice.dto.NotificationRequest;
import com.hireconnect.notificationservice.entity.NotificationLog;
import com.hireconnect.notificationservice.service.NotificationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notify")
public class NotificationController {

    @Autowired
    private NotificationService service;

    @PostMapping("/send-registration-otp")
    public ApiResponse sendRegistrationOtp(@RequestBody EmailOtpRequest request) {
        ApiResponse response = new ApiResponse();
        response.setMessage(service.sendRegistrationOtp(request.getEmail()));
        response.setSuccess(true);
        return response;
    }

    @PostMapping("/verify-registration-otp")
    public ApiResponse verifyRegistrationOtp(@RequestBody EmailOtpRequest request) {
        ApiResponse response = new ApiResponse();
        response.setMessage(service.verifyRegistrationOtp(request.getEmail(), request.getOtp()));
        response.setSuccess(true);
        return response;
    }

    @PostMapping("/send-password-reset-otp")
    public ApiResponse sendPasswordResetOtp(@RequestBody EmailOtpRequest request) {
        ApiResponse response = new ApiResponse();
        response.setMessage(service.sendPasswordResetOtp(request.getEmail()));
        response.setSuccess(true);
        return response;
    }

    @PostMapping("/verify-password-reset-otp")
    public ApiResponse verifyPasswordResetOtp(@RequestBody EmailOtpRequest request) {
        ApiResponse response = new ApiResponse();
        response.setMessage(service.verifyPasswordResetOtp(request.getEmail(), request.getOtp()));
        response.setSuccess(true);
        return response;
    }

    @PostMapping("/send")
    public ApiResponse send(@RequestBody NotificationRequest request) {

        String result = service.sendNotification(request);

        ApiResponse response = new ApiResponse();
        response.setMessage(result);
        response.setSuccess(true);

        return response;
    }

    @PostMapping("/send-async")
    public ApiResponse sendAsync(@RequestBody NotificationRequest request) {

        String result = service.queueNotification(request);

        ApiResponse response = new ApiResponse();
        response.setMessage(result);
        response.setSuccess(true);

        return response;
    }

    @GetMapping("/user/{userId}")
    public List<NotificationLog> getByUser(@PathVariable Long userId,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return service.getNotifications(userId, unreadOnly);
    }

    @GetMapping("/user/{userId}/unread-count")
    public Map<String, Long> unreadCount(@PathVariable Long userId) {
        return Map.of("unreadCount", service.getUnreadCount(userId));
    }

    @PatchMapping("/{id}/read")
    public NotificationLog markRead(@PathVariable Long id) {
        return service.markAsRead(id);
    }

    @PatchMapping("/user/{userId}/read-all")
    public Map<String, Integer> markAllRead(@PathVariable Long userId) {
        return Map.of("updated", service.markAllAsRead(userId));
    }

    @PostMapping("/new-job-alert")
    public ApiResponse newJobAlert(@RequestBody NewJobAlertRequest request) {
        int sent = service.sendNewJobAlert(request);

        ApiResponse response = new ApiResponse();
        response.setMessage("New job alert sent to " + sent + " candidate(s)");
        response.setSuccess(true);

        return response;
    }
}
