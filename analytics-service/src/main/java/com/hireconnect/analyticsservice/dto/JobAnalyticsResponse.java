package com.hireconnect.analyticsservice.dto;

public class JobAnalyticsResponse {

    private Long jobId;
    private String title;
    private String status;
    private Long viewCount;
    private Long applicationCount;
    private Double viewToApplyRatio;
    private Double averageTimeToHireDays;

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }

    public Long getApplicationCount() { return applicationCount; }
    public void setApplicationCount(Long applicationCount) { this.applicationCount = applicationCount; }

    public Double getViewToApplyRatio() { return viewToApplyRatio; }
    public void setViewToApplyRatio(Double viewToApplyRatio) { this.viewToApplyRatio = viewToApplyRatio; }

    public Double getAverageTimeToHireDays() { return averageTimeToHireDays; }
    public void setAverageTimeToHireDays(Double averageTimeToHireDays) { this.averageTimeToHireDays = averageTimeToHireDays; }
}
