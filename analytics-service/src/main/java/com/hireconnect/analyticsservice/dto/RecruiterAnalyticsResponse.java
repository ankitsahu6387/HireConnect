package com.hireconnect.analyticsservice.dto;

import java.util.List;

public class RecruiterAnalyticsResponse {

    private Long recruiterId;
    private Long totalJobs;
    private Long totalViews;
    private Long totalApplications;
    private Double averageViewToApplyRatio;
    private Double averageTimeToHireDays;
    private List<JobAnalyticsResponse> jobs;

    public Long getRecruiterId() { return recruiterId; }
    public void setRecruiterId(Long recruiterId) { this.recruiterId = recruiterId; }

    public Long getTotalJobs() { return totalJobs; }
    public void setTotalJobs(Long totalJobs) { this.totalJobs = totalJobs; }

    public Long getTotalViews() { return totalViews; }
    public void setTotalViews(Long totalViews) { this.totalViews = totalViews; }

    public Long getTotalApplications() { return totalApplications; }
    public void setTotalApplications(Long totalApplications) { this.totalApplications = totalApplications; }

    public Double getAverageViewToApplyRatio() { return averageViewToApplyRatio; }
    public void setAverageViewToApplyRatio(Double averageViewToApplyRatio) { this.averageViewToApplyRatio = averageViewToApplyRatio; }

    public Double getAverageTimeToHireDays() { return averageTimeToHireDays; }
    public void setAverageTimeToHireDays(Double averageTimeToHireDays) { this.averageTimeToHireDays = averageTimeToHireDays; }

    public List<JobAnalyticsResponse> getJobs() { return jobs; }
    public void setJobs(List<JobAnalyticsResponse> jobs) { this.jobs = jobs; }
}
