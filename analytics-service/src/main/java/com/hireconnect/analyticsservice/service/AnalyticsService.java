package com.hireconnect.analyticsservice.service;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hireconnect.analyticsservice.client.*;
import com.hireconnect.analyticsservice.dto.*;
import com.hireconnect.analyticsservice.exception.AnalyticsException;

@Service
public class AnalyticsService {

    @Autowired
    private UserClient userClient;

    @Autowired
    private JobClient jobClient;

    @Autowired
    private InterviewClient interviewClient;

    @Autowired
    private SubscriptionClient subscriptionClient;

    @Autowired
    private ApplicationClient applicationClient;

    public AnalyticsResponse getDashboardData() {

        try {
            AnalyticsResponse response = new AnalyticsResponse();

            response.setTotalUsers(userClient.getUserCount());
            response.setTotalJobs(jobClient.getJobCount());
            response.setTotalApplications(applicationClient.getApplicationCount());
            response.setTotalInterviews(interviewClient.getInterviewCount());
            response.setPremiumUsers(subscriptionClient.getPremiumUserCount());

            return response;

        } catch (Exception e) {
        	e.printStackTrace();
            throw new AnalyticsException("Failed to fetch analytics data: " + e.getMessage());
        }
    }

    public RecruiterAnalyticsResponse getRecruiterAnalytics(Long recruiterId) {
        try {
            List<JobSummary> jobs = jobClient.getJobsByEmployer(recruiterId);
            if (jobs == null) {
                jobs = List.of();
            }
            List<JobAnalyticsResponse> jobReports = new ArrayList<>();

            long totalViews = 0;
            long totalApplications = 0;
            double ratioSum = 0;
            int ratioCount = 0;
            double timeToHireSum = 0;
            int timeToHireCount = 0;

            for (JobSummary job : jobs) {
                List<ApplicationSummary> applications = applicationClient.getApplicationsByJob(job.getId());
                long viewCount = job.getViewCount() == null ? 0L : job.getViewCount();
                long applicationCount = applications == null ? 0L : applications.size();
                Double ratio = viewCount == 0 ? 0.0 : round((applicationCount * 100.0) / viewCount);
                Double averageTimeToHire = calculateAverageTimeToHire(job, applications);

                JobAnalyticsResponse report = new JobAnalyticsResponse();
                report.setJobId(job.getId());
                report.setTitle(job.getTitle());
                report.setStatus(job.getStatus());
                report.setViewCount(viewCount);
                report.setApplicationCount(applicationCount);
                report.setViewToApplyRatio(ratio);
                report.setAverageTimeToHireDays(averageTimeToHire);
                jobReports.add(report);

                totalViews += viewCount;
                totalApplications += applicationCount;
                ratioSum += ratio;
                ratioCount++;
                if (averageTimeToHire != null) {
                    timeToHireSum += averageTimeToHire;
                    timeToHireCount++;
                }
            }

            RecruiterAnalyticsResponse response = new RecruiterAnalyticsResponse();
            response.setRecruiterId(recruiterId);
            response.setTotalJobs((long) jobs.size());
            response.setTotalViews(totalViews);
            response.setTotalApplications(totalApplications);
            response.setAverageViewToApplyRatio(ratioCount == 0 ? 0.0 : round(ratioSum / ratioCount));
            response.setAverageTimeToHireDays(timeToHireCount == 0 ? null : round(timeToHireSum / timeToHireCount));
            response.setJobs(jobReports);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            throw new AnalyticsException("Failed to fetch recruiter analytics data: " + e.getMessage());
        }
    }

    private Double calculateAverageTimeToHire(JobSummary job, List<ApplicationSummary> applications) {
        if (job.getPostedAt() == null || applications == null) {
            return null;
        }

        List<Long> hiredDurations = applications.stream()
                .filter(application -> isHiredStatus(application.getStatus()))
                .map(ApplicationSummary::getAppliedAt)
                .filter(Objects::nonNull)
                .map(appliedAt -> ChronoUnit.DAYS.between(job.getPostedAt(), appliedAt))
                .filter(days -> days >= 0)
                .toList();

        if (hiredDurations.isEmpty()) {
            return null;
        }

        return round(hiredDurations.stream().mapToLong(Long::longValue).average().orElse(0.0));
    }

    private boolean isHiredStatus(String status) {
        return status != null && (status.equalsIgnoreCase("OFFERED") || status.equalsIgnoreCase("HIRED"));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
