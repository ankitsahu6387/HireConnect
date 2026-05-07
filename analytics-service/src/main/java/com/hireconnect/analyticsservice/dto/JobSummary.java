package com.hireconnect.analyticsservice.dto;

import java.time.LocalDateTime;

public class JobSummary {

    private Long id;
    private Long employerId;
    private String title;
    private String status;
    private Long viewCount;
    private LocalDateTime postedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmployerId() { return employerId; }
    public void setEmployerId(Long employerId) { this.employerId = employerId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }

    public LocalDateTime getPostedAt() { return postedAt; }
    public void setPostedAt(LocalDateTime postedAt) { this.postedAt = postedAt; }
}
