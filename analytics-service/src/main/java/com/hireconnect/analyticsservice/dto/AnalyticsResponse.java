package com.hireconnect.analyticsservice.dto;

public class AnalyticsResponse {

    private Long totalUsers;
    private Long totalJobs;
    private Long totalApplications;
    private Long totalInterviews;
    private Long premiumUsers;

    public AnalyticsResponse() {
    }

    public Long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(Long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public Long getTotalJobs() {
        return totalJobs;
    }

    public void setTotalJobs(Long totalJobs) {
        this.totalJobs = totalJobs;
    }

    public Long getTotalApplications() {
        return totalApplications;
    }

    public void setTotalApplications(Long totalApplications) {
        this.totalApplications = totalApplications;
    }

    public Long getTotalInterviews() {
        return totalInterviews;
    }

    public void setTotalInterviews(Long totalInterviews) {
        this.totalInterviews = totalInterviews;
    }

    public Long getPremiumUsers() {
        return premiumUsers;
    }

    public void setPremiumUsers(Long premiumUsers) {
        this.premiumUsers = premiumUsers;
    }
}
