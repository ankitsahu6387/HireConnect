package com.hireconnect.notificationservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.hireconnect.notificationservice.dto.ApiResponse;
import com.hireconnect.notificationservice.dto.EmailOtpRequest;
import com.hireconnect.notificationservice.dto.NewJobAlertRequest;
import com.hireconnect.notificationservice.dto.NotificationRequest;
import com.hireconnect.notificationservice.entity.NotificationLog;
import com.hireconnect.notificationservice.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService service;

    private NotificationController controller;

    @BeforeEach
    void setUp() {
        controller = new NotificationController();
        ReflectionTestUtils.setField(controller, "service", service);
    }

    @Test
    void otpEndpointsReturnSuccessfulResponses() {
        EmailOtpRequest request = new EmailOtpRequest();
        request.setEmail("user@example.com");
        request.setOtp("123456");

        when(service.sendRegistrationOtp("user@example.com")).thenReturn("registration sent");
        when(service.verifyRegistrationOtp("user@example.com", "123456")).thenReturn("registration verified");
        when(service.sendPasswordResetOtp("user@example.com")).thenReturn("reset sent");
        when(service.verifyPasswordResetOtp("user@example.com", "123456")).thenReturn("reset verified");

        assertResponse(controller.sendRegistrationOtp(request), "registration sent");
        assertResponse(controller.verifyRegistrationOtp(request), "registration verified");
        assertResponse(controller.sendPasswordResetOtp(request), "reset sent");
        assertResponse(controller.verifyPasswordResetOtp(request), "reset verified");
    }

    @Test
    void notificationCommandEndpointsReturnSuccessfulResponses() {
        NotificationRequest request = new NotificationRequest();
        when(service.sendNotification(request)).thenReturn("sent");
        when(service.queueNotification(request)).thenReturn("queued");

        assertResponse(controller.send(request), "sent");
        assertResponse(controller.sendAsync(request), "queued");
    }

    @Test
    void readEndpointsDelegateToService() {
        NotificationLog notification = new NotificationLog();
        when(service.getNotifications(5L, true)).thenReturn(List.of(notification));
        when(service.getUnreadCount(5L)).thenReturn(3L);
        when(service.markAsRead(9L)).thenReturn(notification);
        when(service.markAllAsRead(5L)).thenReturn(2);

        assertThat(controller.getByUser(5L, true)).containsExactly(notification);
        assertThat(controller.unreadCount(5L)).containsEntry("unreadCount", 3L);
        assertThat(controller.markRead(9L)).isSameAs(notification);
        assertThat(controller.markAllRead(5L)).containsEntry("updated", 2);
    }

    @Test
    void newJobAlertReportsSentCount() {
        NewJobAlertRequest request = new NewJobAlertRequest();
        when(service.sendNewJobAlert(request)).thenReturn(4);

        assertResponse(controller.newJobAlert(request), "New job alert sent to 4 candidate(s)");
    }

    private static void assertResponse(ApiResponse response, String message) {
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo(message);
    }
}
