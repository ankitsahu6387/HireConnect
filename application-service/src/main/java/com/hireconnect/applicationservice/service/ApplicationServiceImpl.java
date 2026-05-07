package com.hireconnect.applicationservice.service;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.hireconnect.applicationservice.dto.ApplicationDTO;
import com.hireconnect.applicationservice.entity.Application;
import com.hireconnect.applicationservice.exception.ResourceNotFoundException;
import com.hireconnect.applicationservice.exception.InvalidStatusException;
import com.hireconnect.applicationservice.exception.DuplicateApplicationException;
import com.hireconnect.applicationservice.repository.ApplicationRepository;

@Service
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository repository;
    private final RestTemplate restTemplate = new RestTemplate();

    public ApplicationServiceImpl(ApplicationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Application applyJob(ApplicationDTO dto) {

        // Duplicate apply check
        if (repository.findByUserIdAndJobId(dto.getUserId(), dto.getJobId()).isPresent()) {
            throw new DuplicateApplicationException("Already applied to this job");
        }
        Application app = new Application(
                dto.getJobId(),
                dto.getUserId(),
                dto.getResumeLink(),
                "APPLIED"
        );
        app.setCoverLetter(dto.getCoverLetter());

        return repository.save(app);
    }

    @Override
    public List<Application> getApplicationsByUser(Long userId) {
        return repository.findByUserId(userId);
    }

    @Override
    public List<Application> getApplicationsByJob(Long jobId) {
        return repository.findByJobId(jobId);
    }

    @Override
    public Application updateStatus(Long id, String status) {

        Application app = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        String normalizedStatus = status == null ? "" : status.trim().toUpperCase().replace(" ", "_");

        if (!normalizedStatus.equals("APPLIED") &&
            !normalizedStatus.equals("SHORTLISTED") &&
            !normalizedStatus.equals("INTERVIEW_SCHEDULED") &&
            !normalizedStatus.equals("OFFERED") &&
            !normalizedStatus.equals("REJECTED") &&
            !normalizedStatus.equals("WITHDRAWN")) {

            throw new InvalidStatusException("Invalid status value");
        }

        app.setStatus(normalizedStatus);
        Application saved = repository.save(app);
        notifyStatusChange(saved);
        return saved;
    }

    private void notifyStatusChange(Application app) {
        try {
            Map<?, ?> job = getJob(app.getJobId());
            String jobLabel = getJobLabel(job);
            String subject = statusSubject(app.getStatus());
            String message = buildStatusMessage(app, job, jobLabel);
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", app.getUserId());
            payload.put("type", "APPLICATION_STATUS_CHANGE");
            payload.put("subject", subject);
            payload.put("message", message);
            payload.put("sendEmail", shouldEmailStatus(app.getStatus()));
            restTemplate.postForObject("http://localhost:8086/notify/send", payload, Object.class);
        } catch (Exception ignored) {
            // Notification delivery should not block application status changes.
        }
    }

    private String getJobLabel(Long jobId) {
        if (jobId == null) {
            return "this job";
        }

        try {
            return getJobLabel(getJob(jobId));
        } catch (Exception ignored) {
            // Fall through to generic wording.
        }

        return "this job";
    }

    private Map<?, ?> getJob(Long jobId) {
        if (jobId == null) {
            return null;
        }
        return restTemplate.getForObject("http://localhost:8083/jobs/" + jobId, Map.class);
    }

    private String getJobLabel(Map<?, ?> job) {
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

        return "this job";
    }

    private String getCandidateLabel(Long userId) {
        if (userId == null) {
            return "A candidate";
        }

        try {
            Map<?, ?> user = restTemplate.getForObject("http://localhost:8082/users/" + userId, Map.class);
            String name = stringValue(user == null ? null : user.get("name"));
            String email = stringValue(user == null ? null : user.get("email"));

            if (!name.isBlank()) {
                return name;
            }
            if (!email.isBlank()) {
                return email;
            }
        } catch (Exception ignored) {
            // Fall through to generic wording.
        }

        return "A candidate";
    }

    private String statusSubject(String status) {
        if ("SHORTLISTED".equalsIgnoreCase(status)) {
            return "You have been shortlisted";
        }
        if ("OFFERED".equalsIgnoreCase(status)) {
            return "Offer received";
        }
        if ("REJECTED".equalsIgnoreCase(status)) {
            return "Application update";
        }
        return "Application status updated";
    }

    private boolean shouldEmailStatus(String status) {
        return "SHORTLISTED".equalsIgnoreCase(status)
                || "OFFERED".equalsIgnoreCase(status)
                || "REJECTED".equalsIgnoreCase(status);
    }

    private String buildStatusMessage(Application app, Map<?, ?> job, String jobLabel) {
        String userName = getCandidateLabel(app.getUserId());
        String jobTitle = emptyFallback(stringValue(job == null ? null : job.get("title")), "this role");
        String companyName = emptyFallback(stringValue(job == null ? null : job.get("companyName")), "the company");

        if ("SHORTLISTED".equalsIgnoreCase(app.getStatus())) {
            return "Hi " + userName + ",\n\n"
                    + "Great news!\n"
                    + "You have been shortlisted for the position of " + jobTitle + " at " + companyName + ".\n"
                    + "Our recruitment team is currently reviewing your profile, and you may be contacted soon for the next steps.\n\n"
                    + "Stay prepared and keep an eye on your dashboard for updates.\n\n"
                    + "Best regards,\n"
                    + "Team HireConnect.";
        }

        if ("OFFERED".equalsIgnoreCase(app.getStatus())) {
            return "Hi " + userName + ",\n\n"
                    + "Congratulations!\n"
                    + "You have received an offer for " + jobTitle + " at " + companyName + ".\n\n"
                    + "Please log in to your dashboard to view the offer details.\n\n"
                    + "Best regards,\n"
                    + "Team HireConnect.";
        }

        if ("REJECTED".equalsIgnoreCase(app.getStatus())) {
            return "Hi " + userName + ",\n\n"
                    + "Thank you for applying for " + jobTitle + " at " + companyName + ".\n"
                    + "After careful consideration, we regret to inform you that you were not selected for this role.\n"
                    + "We encourage you to apply for other opportunities on HireConnect.\n\n"
                    + "Best wishes for your future.\n"
                    + "Team HireConnect.";
        }

        return "Your application for " + jobLabel + " is now " + app.getStatus() + ".";
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String emptyFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

}
