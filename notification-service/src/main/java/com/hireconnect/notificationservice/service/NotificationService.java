package com.hireconnect.notificationservice.service;

import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.hireconnect.notificationservice.dto.NewJobAlertRequest;
import com.hireconnect.notificationservice.dto.NotificationRequest;
import com.hireconnect.notificationservice.entity.NotificationLog;
import com.hireconnect.notificationservice.exception.NotificationException;
import com.hireconnect.notificationservice.repository.NotificationRepository;

@Service
public class NotificationService {

    private static final long OTP_VALID_MINUTES = 10;
    private static final SecureRandom OTP_RANDOM = new SecureRandom();
    private static final List<String> EMPLOYER_NOTIFICATION_TYPES = List.of(
            "JOB_POSTED",
            "INTERVIEW_RESCHEDULE_REQUESTED"
    );

    @Autowired
    private NotificationRepository repository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private RestTemplate restTemplate;

    private final Map<String, OtpEntry> registrationOtps = new ConcurrentHashMap<>();
    private final Map<String, OtpEntry> passwordResetOtps = new ConcurrentHashMap<>();

    public String sendRegistrationOtp(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isBlank()) {
            throw new NotificationException("Email is required");
        }

        String otp = String.valueOf(100000 + OTP_RANDOM.nextInt(900000));
        System.out.println("GENERATED OTP: " + otp);  // DEBUG
        registrationOtps.put(normalizedEmail, new OtpEntry(otp, LocalDateTime.now().plusMinutes(OTP_VALID_MINUTES)));

        sendEmail(
                normalizedEmail,
                "Your HireConnect verification code",
                "Your HireConnect signup verification code is " + otp + ".\n\n"
                        + "This code will expire in " + OTP_VALID_MINUTES + " minutes.\n\n"
                        + "If you did not request this code, you can ignore this email.\n\n"
                        + "Best regards,\n"
                        + "Team HireConnect"
        );

        return "Verification OTP sent successfully";
    }

    public String verifyRegistrationOtp(String email, String otp) {
        return verifyOtp(registrationOtps, email, otp);
    }

    public String sendPasswordResetOtp(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isBlank()) {
            throw new NotificationException("Email is required");
        }

        String otp = String.valueOf(100000 + OTP_RANDOM.nextInt(900000));
        passwordResetOtps.put(normalizedEmail, new OtpEntry(otp, LocalDateTime.now().plusMinutes(OTP_VALID_MINUTES)));

        sendEmail(
                normalizedEmail,
                "Your HireConnect password reset code",
                "Your HireConnect password reset code is " + otp + ".\n\n"
                        + "This code will expire in " + OTP_VALID_MINUTES + " minutes.\n\n"
                        + "If you did not request this code, you can ignore this email\n\n"
                        + "Best regards,\n"
                        + "Team HireConnect"
        );

        return "Password reset OTP sent successfully";
    }

    public String verifyPasswordResetOtp(String email, String otp) {
        return verifyOtp(passwordResetOtps, email, otp);
    }

    private String verifyOtp(Map<String, OtpEntry> otpStore, String email, String otp) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isBlank() || otp == null || otp.isBlank()) {
            throw new NotificationException("Email and OTP are required");
        }

        OtpEntry entry = otpStore.get(normalizedEmail);
        if (entry == null) {
            throw new NotificationException("OTP not found or expired");
        }

        if (LocalDateTime.now().isAfter(entry.expiresAt())) {
            otpStore.remove(normalizedEmail);
            throw new NotificationException("OTP expired");
        }

        if (!entry.otp().equals(otp.trim())) {
            throw new NotificationException("Invalid OTP");
        }

        otpStore.remove(normalizedEmail);
        return "Email verified successfully";
    }

    public String sendNotification(NotificationRequest request) {

        try {
            String email = resolveEmail(request);
            boolean shouldSendEmail = (request.getSendEmail() == null || request.getSendEmail())
                    && isEmailAllowedForUser(request.getUserId());

            NotificationLog log = new NotificationLog();
            log.setUserId(request.getUserId());
            log.setEmail(email);
            log.setSubject(request.getSubject());
            log.setMessage(request.getMessage());
            log.setType(normalizeType(request.getType()));
            log.setCreatedAt(LocalDateTime.now());

            if (shouldSendEmail && email != null && !email.isBlank()) {
                sendEmail(email, request.getSubject(), request.getMessage());
                log.setEmailSent(true);
                log.setSentAt(LocalDateTime.now());
            }

            repository.save(log);

            return "Notification Sent Successfully";

        } catch (Exception e) {
            throw new NotificationException("Failed to send notification", e);
        }
    }

    public List<NotificationLog> getNotifications(Long userId, boolean unreadOnly) {
        if (isEmployer(userId)) {
            if (unreadOnly) {
                return repository.findByUserIdAndReadFalseAndTypeInOrderByCreatedAtDesc(userId, EMPLOYER_NOTIFICATION_TYPES);
            }
            return repository.findByUserIdAndTypeInOrderByCreatedAtDesc(userId, EMPLOYER_NOTIFICATION_TYPES);
        }

        if (unreadOnly) {
            return repository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        }
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public long getUnreadCount(Long userId) {
        if (isEmployer(userId)) {
            return repository.countByUserIdAndReadFalseAndTypeIn(userId, EMPLOYER_NOTIFICATION_TYPES);
        }
        return repository.countByUserIdAndReadFalse(userId);
    }

    public NotificationLog markAsRead(Long id) {
        NotificationLog notification = repository.findById(id)
                .orElseThrow(() -> new NotificationException("Notification not found"));
        notification.setRead(true);
        return repository.save(notification);
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        if (isEmployer(userId)) {
            return repository.markAllReadByUserIdAndTypeIn(userId, EMPLOYER_NOTIFICATION_TYPES);
        }
        return repository.markAllReadByUserId(userId);
    }

    public int sendNewJobAlert(NewJobAlertRequest request) {
        List<Map<String, Object>> users = getUsers();
        int sent = 0;

        for (Map<String, Object> user : users) {
            String role = String.valueOf(user.getOrDefault("role", ""));
            if (!"CANDIDATE".equalsIgnoreCase(role) && !"JOB_SEEKER".equalsIgnoreCase(role)) {
                continue;
            }

            NotificationRequest notification = new NotificationRequest();
            notification.setUserId(toLong(user.get("id")));
            notification.setEmail((String) user.get("email"));
            notification.setType("NEW_JOB_ALERT");
            notification.setSubject("New job posted: " + emptyFallback(request.getTitle(), "Open role")
                    + " at " + emptyFallback(request.getCompanyName(), "Company not specified"));
            notification.setMessage(buildNewJobMessage(request, displayName(user)));
            notification.setSendEmail(hasActiveCandidateSubscription(notification.getUserId()));
            sendNotification(notification);
            sent++;
        }

        return sent;
    }

    private void sendEmail(String email, String subject, String body) {

    try {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);

        System.out.println("EMAIL SENT SUCCESSFULLY to: " + email);

    } catch (Exception e) {
        e.printStackTrace();
        throw new NotificationException("Email sending failed", e);
    }
}

    private String resolveEmail(NotificationRequest request) {
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            return request.getEmail();
        }

        if (request.getUserId() == null) {
            return null;
        }

        try {
            Map<?, ?> user = restTemplate.getForObject("http://localhost:8082/users/" + request.getUserId(), Map.class);
            if (user == null || user.get("email") == null) {
                return null;
            }
            return String.valueOf(user.get("email"));
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<Map<String, Object>> getUsers() {
        try {
            Map<String, Object>[] users = restTemplate.getForObject("http://localhost:8082/users", Map[].class);
            if (users == null) {
                return List.of();
            }
            return Arrays.asList(users);
        } catch (Exception e) {
            throw new NotificationException("Unable to load users for new job alerts", e);
        }
    }

    private String normalizeType(String type) {
        return type == null || type.isBlank() ? "GENERAL" : type.trim().toUpperCase().replace(" ", "_");
    }

    private String buildNewJobMessage(NewJobAlertRequest request, String userName) {
        return "Hi " + emptyFallback(userName, "there") + ",\n\n"
                + "We found a new job that matches your profile.\n"
                + "Role: " + emptyFallback(request.getTitle(), "Open role") + "\n"
                + "Company: " + emptyFallback(request.getCompanyName(), "Not specified") + "\n"
                + "Location: " + emptyFallback(request.getLocation(), "Remote") + "\n"
                + "See your dashboard for more details.\n\n"
                + "Best regards,\n"
                + "Team HireConnect.";
    }

    private String displayName(Map<String, Object> user) {
        if (user == null) {
            return "there";
        }

        String name = stringValue(user.get("name"));
        if (!name.isBlank()) {
            return name;
        }

        String username = stringValue(user.get("username"));
        if (!username.isBlank()) {
            return username;
        }

        String email = stringValue(user.get("email"));
        int atIndex = email.indexOf('@');
        if (atIndex > 0) {
            return email.substring(0, atIndex);
        }

        return "there";
    }

    private boolean hasActiveCandidateSubscription(Long userId) {
        if (userId == null) {
            return false;
        }

        try {
            Map<?, ?> response = restTemplate.getForObject("http://localhost:8087/subscription/" + userId, Map.class);
            Object data = response == null ? null : response.get("data");
            if (!(data instanceof Map<?, ?> subscription)) {
                return false;
            }

            String status = subscription.get("status") == null ? "" : String.valueOf(subscription.get("status"));
            String plan = subscription.get("plan") == null ? "" : String.valueOf(subscription.get("plan"));
            return "ACTIVE".equalsIgnoreCase(status)
                    && ("PREMIUM".equalsIgnoreCase(plan)
                    || "PROFESSIONAL".equalsIgnoreCase(plan)
                    || "ENTERPRISE".equalsIgnoreCase(plan));
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isEmailAllowedForUser(Long userId) {
        if (userId == null) {
            return true;
        }

        try {
            Map<?, ?> user = restTemplate.getForObject("http://localhost:8082/users/" + userId, Map.class);
            String role = user == null || user.get("role") == null ? "" : String.valueOf(user.get("role"));
            if ("CANDIDATE".equalsIgnoreCase(role) || "JOB_SEEKER".equalsIgnoreCase(role)) {
                return hasActiveCandidateSubscription(userId);
            }
            if ("EMPLOYER".equalsIgnoreCase(role)) {
                return false;
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isEmployer(Long userId) {
        if (userId == null) {
            return false;
        }

        try {
            Map<?, ?> user = restTemplate.getForObject("http://localhost:8082/users/" + userId, Map.class);
            String role = user == null || user.get("role") == null ? "" : String.valueOf(user.get("role"));
            return "EMPLOYER".equalsIgnoreCase(role);
        } catch (Exception ignored) {
            return false;
        }
    }

    private String emptyFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private record OtpEntry(String otp, LocalDateTime expiresAt) {
    }
}
