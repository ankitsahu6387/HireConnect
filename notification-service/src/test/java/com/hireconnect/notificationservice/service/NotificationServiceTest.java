package com.hireconnect.notificationservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hireconnect.notificationservice.dto.NewJobAlertRequest;
import com.hireconnect.notificationservice.dto.NotificationRequest;
import com.hireconnect.notificationservice.entity.NotificationLog;
import com.hireconnect.notificationservice.exception.NotificationException;
import com.hireconnect.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private NotificationService service;

    @Test
    void sendAndVerifyRegistrationOtpUsesGeneratedCode() {
        service.sendRegistrationOtp(" USER@Example.COM ");
        String otp = sentOtp();

        assertThat(service.verifyRegistrationOtp("user@example.com", otp))
                .isEqualTo("Email verified successfully");

        assertThatThrownBy(() -> service.verifyRegistrationOtp("user@example.com", otp))
                .isInstanceOf(NotificationException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void verifyPasswordResetOtpRejectsInvalidCode() {
        service.sendPasswordResetOtp("user@example.com");

        assertThatThrownBy(() -> service.verifyPasswordResetOtp("user@example.com", "000000"))
                .isInstanceOf(NotificationException.class)
                .hasMessageContaining("Invalid OTP");
    }

    @Test
    void sendOtpRequiresEmail() {
        assertThatThrownBy(() -> service.sendRegistrationOtp(" "))
                .isInstanceOf(NotificationException.class)
                .hasMessageContaining("Email is required");
    }

    @Test
    void sendNotificationSavesLogWithoutEmailWhenSendEmailFalse() {
        NotificationRequest request = request();
        request.setType("application update");
        request.setSendEmail(false);
        when(repository.save(any(NotificationLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String result = service.sendNotification(request);

        assertThat(result).isEqualTo("Notification Sent Successfully");
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("APPLICATION_UPDATE");
        assertThat(captor.getValue().isEmailSent()).isFalse();
    }

    @Test
    void sendNotificationValidatesRequiredFields() {
        NotificationRequest request = request();
        request.setSubject(" ");

        assertThatThrownBy(() -> service.sendNotification(request))
                .isInstanceOf(NotificationException.class)
                .hasMessageContaining("Failed to send notification");
    }

    @Test
    void queueNotificationSendsImmediately() {
        NotificationRequest request = request();
        when(repository.save(any(NotificationLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.queueNotification(request)).isEqualTo("Notification sent successfully");
        verify(repository).save(any(NotificationLog.class));
    }

    @Test
    void getNotificationsUsesEmployerFilteredQueries() {
        NotificationLog log = new NotificationLog();
        when(restTemplate.getForObject("http://user-service/users/4", Map.class))
                .thenReturn(Map.of("role", "EMPLOYER"));
        when(repository.findByUserIdAndReadFalseAndTypeInOrderByCreatedAtDesc(eq(4L), any()))
                .thenReturn(List.of(log));
        when(repository.countByUserIdAndReadFalseAndTypeIn(eq(4L), any())).thenReturn(2L);
        when(repository.markAllReadByUserIdAndTypeIn(eq(4L), any())).thenReturn(2);

        assertThat(service.getNotifications(4L, true)).containsExactly(log);
        assertThat(service.getUnreadCount(4L)).isEqualTo(2L);
        assertThat(service.markAllAsRead(4L)).isEqualTo(2);
    }

    @Test
    void markAsReadSetsReadFlag() {
        NotificationLog log = new NotificationLog();
        when(repository.findById(8L)).thenReturn(Optional.of(log));
        when(repository.save(log)).thenReturn(log);

        NotificationLog result = service.markAsRead(8L);

        assertThat(result.isRead()).isTrue();
    }

    @Test
    void sendNewJobAlertSendsOnlyCandidateAlerts() {
        Map<String, Object> candidate = Map.of(
                "id", 1,
                "name", "Ankit",
                "email", "ankit@example.com",
                "role", "CANDIDATE"
        );
        Map<String, Object> employer = Map.of(
                "id", 2,
                "name", "Employer",
                "email", "employer@example.com",
                "role", "EMPLOYER"
        );
        when(restTemplate.getForObject("http://user-service/users", Map[].class))
                .thenReturn(new Map[] { candidate, employer });
        when(restTemplate.getForObject("http://subscription-service/subscription/1", Map.class))
                .thenReturn(Map.of("data", Map.of("status", "ACTIVE", "plan", "PROFESSIONAL")));
        when(restTemplate.getForObject("http://user-service/users/1", Map.class))
                .thenReturn(Map.of("role", "CANDIDATE"));
        when(repository.save(any(NotificationLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int sent = service.sendNewJobAlert(newJobAlert());

        assertThat(sent).isEqualTo(1);
    }

    private NotificationRequest request() {
        NotificationRequest request = new NotificationRequest();
        request.setUserId(1L);
        request.setEmail("user@example.com");
        request.setSubject("Subject");
        request.setMessage("Message");
        request.setType("GENERAL");
        request.setSendEmail(false);
        return request;
    }

    private NewJobAlertRequest newJobAlert() {
        NewJobAlertRequest request = new NewJobAlertRequest();
        request.setJobId(10L);
        request.setTitle("Java Developer");
        request.setCompanyName("HireConnect");
        request.setLocation("Remote");
        request.setCategory("IT");
        return request;
    }

    private String sentOtp() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        Matcher matcher = Pattern.compile("\\b\\d{6}\\b").matcher(captor.getValue().getText());
        assertThat(matcher.find()).isTrue();
        return matcher.group();
    }
}
