package com.hireconnect.interviewservice.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.hireconnect.interviewservice.dto.InterviewDTO;
import com.hireconnect.interviewservice.dto.RescheduleRequestDTO;
import com.hireconnect.interviewservice.entity.Interview;
import com.hireconnect.interviewservice.exception.ResourceNotFoundException;
import com.hireconnect.interviewservice.exception.InvalidInterviewStatusException;
import com.hireconnect.interviewservice.repository.InterviewRepository;

@Service
public class InterviewServiceImpl implements InterviewService {

    private final InterviewRepository repository;
    private final RestTemplate restTemplate;

    public InterviewServiceImpl(InterviewRepository repository, RestTemplate restTemplate) {
        this.repository = repository;
        this.restTemplate = restTemplate;
    }

    @Override
    public Interview scheduleInterview(InterviewDTO dto) {
        if (!isFutureDateTime(dto.getInterviewDate())) {
            throw new InvalidInterviewStatusException("Interview date must be in the future");
        }

        Interview interview = new Interview(
                dto.getApplicationId(),
                dto.getJobId(),
                dto.getUserId(),
                dto.getRecruiterId(),
                dto.getInterviewDate(),
                normalizeMode(dto.getMode()),
                dto.getLocation(),
                dto.getMeetingLink(),
                dto.getNotes(),
                "SCHEDULED"
        );
        Interview saved = repository.save(interview);
        Map<?, ?> job = getJob(saved.getJobId());
        notifyUser(saved.getUserId(), "INTERVIEW_SCHEDULED", "Interview scheduled",
                buildInterviewScheduledMessage(saved, job));
        return saved;
    }

    @Override
    public List<Interview> getByUser(Long userId) {
        return repository.findByUserId(userId);
    }

    @Override
    public List<Interview> getByJob(Long jobId) {
        return repository.findByJobId(jobId);
    }

    @Override
    public List<Interview> getByApplication(Long applicationId) {
        return repository.findByApplicationId(applicationId);
    }

    @Override
    public Interview updateStatus(Long id, String status) {

        Interview interview = getInterview(id);
        interview.setStatus(normalizeStatus(status));
        return repository.save(interview);
    }

    @Override
    public Interview confirmInterview(Long id) {
        Interview interview = getInterview(id);
        String currentStatus = normalizeStatus(interview.getStatus());

        if (!currentStatus.equals("SCHEDULED") && !currentStatus.equals("RESCHEDULED")) {
            throw new InvalidInterviewStatusException("Only scheduled interviews can be confirmed");
        }

        interview.setStatus("CONFIRMED");
        return repository.save(interview);
    }

    @Override
    public Interview requestReschedule(Long id, RescheduleRequestDTO dto) {
        Interview interview = getInterview(id);
        String currentStatus = normalizeStatus(interview.getStatus());

        if (!currentStatus.equals("SCHEDULED") && !currentStatus.equals("CONFIRMED") && !currentStatus.equals("RESCHEDULED")) {
            throw new InvalidInterviewStatusException("Only active interviews can be rescheduled");
        }

        if (!isFutureDateTime(dto.getRequestedInterviewDate())) {
            throw new InvalidInterviewStatusException("Requested interview date must be in the future");
        }

        interview.setRequestedInterviewDate(dto.getRequestedInterviewDate());
        interview.setRescheduleReason(dto.getReason());
        if (dto.getNotes() != null && !dto.getNotes().trim().isEmpty()) {
            interview.setNotes(dto.getNotes());
        }
        interview.setStatus("RESCHEDULE_REQUESTED");
        Interview saved = repository.save(interview);
        String jobLabel = getJobLabel(saved.getJobId());
        String candidateName = getCandidateName(saved.getUserId());
        notifyUser(saved.getRecruiterId(), "INTERVIEW_RESCHEDULE_REQUESTED", "Interview reschedule requested",
                candidateName + " requested a new interview time for " + jobLabel
                        + ". Earlier scheduled " + formatInterviewDateTime(saved.getInterviewDate())
                        + "\nNew Requested time: " + formatInterviewDateTime(saved.getRequestedInterviewDate())
                        + " Due to Reason: " + ensurePeriod(emptyFallback(saved.getRescheduleReason(), "No reason provided")));
        return saved;
    }

    private void notifyUser(Long userId, String type, String subject, String message) {
        if (userId == null) {
            return;
        }
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("type", type);
            payload.put("subject", subject);
            payload.put("message", message);
            payload.put("sendEmail", true);
            restTemplate.postForObject("http://notification-service/notify/send", payload, Object.class);
        } catch (Exception ignored) {
            // Notification delivery should not block interview workflows.
        }
    }

    private String emptyFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String ensurePeriod(String value) {
        String trimmedValue = emptyFallback(value, "").trim();
        if (trimmedValue.endsWith(".") || trimmedValue.endsWith("!") || trimmedValue.endsWith("?")) {
            return trimmedValue;
        }
        return trimmedValue + ".";
    }

    private String getJobLabel(Long jobId) {
        if (jobId == null) {
            return "this job";
        }

        try {
            Map<?, ?> job = getJob(jobId);
            if (job == null) {
                return "this job";
            }

            String title = stringValue(job.get("title"));
            String companyName = stringValue(job.get("companyName"));

            if (!title.isBlank() && !companyName.isBlank()) {
                return title + " at " + companyName;
            }
            if (!title.isBlank()) {
                return title;
            }
            if (!companyName.isBlank()) {
                return "a role at " + companyName;
            }
        } catch (Exception ignored) {
            // Fall through to generic wording.
        }

        return "this job";
    }

    private Map<?, ?> getJob(Long jobId) {
        if (jobId == null) {
            return null;
        }
        return restTemplate.getForObject("http://job-service/jobs/" + jobId, Map.class);
    }

    private String buildInterviewScheduledMessage(Interview interview, Map<?, ?> job) {
        String userName = getCandidateName(interview.getUserId());
        String jobTitle = emptyFallback(stringValue(job == null ? null : job.get("title")), "this role");
        String companyName = emptyFallback(stringValue(job == null ? null : job.get("companyName")), "the company");

        return "Hi " + userName + ",\n\n"
                + "Congratulations!\n"
                + "Your interview has been scheduled.\n"
                + "Role: " + jobTitle + "\n"
                + "Company: " + companyName + "\n"
                + "Date: " + formatInterviewDate(interview.getInterviewDate()) + "\n"
                + "Time: " + formatInterviewTime(interview.getInterviewDate()) + "\n"
                + "See your dashboard for more details.\n\n"
                + "Please be available on time.\n\n"
                + "Best of luck!\n"
                + "Team HireConnect.";
    }

    private String getCandidateName(Long userId) {
        if (userId == null) {
            return "there";
        }

        try {
            Map<?, ?> user = restTemplate.getForObject("http://user-service/users/" + userId, Map.class);
            String name = stringValue(user == null ? null : user.get("name"));
            String username = stringValue(user == null ? null : user.get("username"));
            String email = stringValue(user == null ? null : user.get("email"));

            if (!name.isBlank()) {
                return name;
            }
            if (!username.isBlank()) {
                return username;
            }
            int atIndex = email.indexOf('@');
            if (atIndex > 0) {
                return email.substring(0, atIndex);
            }
        } catch (Exception ignored) {
            // Fall through to generic wording.
        }

        return "there";
    }

    private String formatInterviewDateTime(String value) {
        LocalDateTime dateTime = parseInterviewDateTime(value);
        if (dateTime == null) {
            return emptyFallback(value, "To be announced");
        }
        return "On " + dateTime.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + " At " + dateTime.toLocalTime().format(DateTimeFormatter.ofPattern("h:mm a"));
    }

    private String formatInterviewDate(String value) {
        LocalDateTime dateTime = parseInterviewDateTime(value);
        if (dateTime == null) {
            return emptyFallback(value, "To be announced");
        }
        return dateTime.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String formatInterviewTime(String value) {
        LocalDateTime dateTime = parseInterviewDateTime(value);
        if (dateTime == null) {
            return emptyFallback(value, "To be announced");
        }
        return dateTime.toLocalTime().format(DateTimeFormatter.ofPattern("h:mm a"));
    }

    private LocalDateTime parseInterviewDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(value).toLocalDateTime();
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private boolean isFutureDateTime(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        LocalDateTime dateTime = parseInterviewDateTime(value);
        return dateTime != null && dateTime.isAfter(LocalDateTime.now());
    }

    private Interview getInterview(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));
    }

    private String normalizeMode(String mode) {
        String normalizedMode = mode == null ? "" : mode.trim().toUpperCase().replace("-", "_").replace(" ", "_");

        if (normalizedMode.equals("ONLINE")) {
            return "ONLINE";
        }

        if (normalizedMode.equals("IN_PERSON") || normalizedMode.equals("OFFLINE")) {
            return "IN_PERSON";
        }

        throw new InvalidInterviewStatusException("Invalid interview mode");
    }

    private String normalizeStatus(String status) {
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase().replace("-", "_").replace(" ", "_");

        if (!normalizedStatus.equals("SCHEDULED") &&
            !normalizedStatus.equals("CONFIRMED") &&
            !normalizedStatus.equals("RESCHEDULE_REQUESTED") &&
            !normalizedStatus.equals("RESCHEDULED") &&
            !normalizedStatus.equals("COMPLETED") &&
            !normalizedStatus.equals("CANCELLED")) {

            throw new InvalidInterviewStatusException("Invalid interview status");
        }

        return normalizedStatus;
    }
}
