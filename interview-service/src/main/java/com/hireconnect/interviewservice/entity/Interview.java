package com.hireconnect.interviewservice.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "interviews")
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long applicationId;
    private Long jobId;
    private Long userId;
    private Long recruiterId;

    private String interviewDate; // keep String for simplicity
    private String requestedInterviewDate;
    private String mode; // ONLINE / IN_PERSON
    private String location;
    private String meetingLink;

    @Column(length = 1000)
    private String notes;

    @Column(length = 1000)
    private String rescheduleReason;

    private String status; // SCHEDULED, CONFIRMED, RESCHEDULE_REQUESTED, COMPLETED, CANCELLED

    public Interview() {}

    public Interview(Long applicationId, Long jobId, Long userId,
                     Long recruiterId, String interviewDate, String mode,
                     String location, String meetingLink, String notes,
                     String status) {
        this.applicationId = applicationId;
        this.jobId = jobId;
        this.userId = userId;
        this.recruiterId = recruiterId;
        this.interviewDate = interviewDate;
        this.mode = mode;
        this.location = location;
        this.meetingLink = meetingLink;
        this.notes = notes;
        this.status = status;
    }

    // Getters & Setters
    public Long getId() { return id; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getRecruiterId() { return recruiterId; }
    public void setRecruiterId(Long recruiterId) { this.recruiterId = recruiterId; }

    public String getInterviewDate() { return interviewDate; }
    public void setInterviewDate(String interviewDate) { this.interviewDate = interviewDate; }

    public String getRequestedInterviewDate() { return requestedInterviewDate; }
    public void setRequestedInterviewDate(String requestedInterviewDate) { this.requestedInterviewDate = requestedInterviewDate; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getMeetingLink() { return meetingLink; }
    public void setMeetingLink(String meetingLink) { this.meetingLink = meetingLink; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getRescheduleReason() { return rescheduleReason; }
    public void setRescheduleReason(String rescheduleReason) { this.rescheduleReason = rescheduleReason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
