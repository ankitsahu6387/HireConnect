package com.hireconnect.applicationservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long jobId;
    private Long userId;

    private String resumeLink;
    private String coverLetter;
    private LocalDateTime appliedAt;

    private String status;

    public Application() {}

    public Application(Long jobId, Long userId, String resumeLink, String status) {
        this.jobId = jobId;
        this.userId = userId;
        this.resumeLink = resumeLink;
        this.status = status;
        this.appliedAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId() { return id; }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getResumeLink() { return resumeLink; }
    public void setResumeLink(String resumeLink) { this.resumeLink = resumeLink; }

    public String getCoverLetter() { return coverLetter; }
    public void setCoverLetter(String coverLetter) { this.coverLetter = coverLetter; }

    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
