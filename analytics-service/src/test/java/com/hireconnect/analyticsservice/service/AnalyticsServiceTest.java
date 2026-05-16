package com.hireconnect.analyticsservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import com.hireconnect.analyticsservice.client.ApplicationClient;
import com.hireconnect.analyticsservice.client.InterviewClient;
import com.hireconnect.analyticsservice.client.JobClient;
import com.hireconnect.analyticsservice.client.SubscriptionClient;
import com.hireconnect.analyticsservice.client.UserClient;
import com.hireconnect.analyticsservice.dto.AnalyticsResponse;
import com.hireconnect.analyticsservice.dto.ApplicationSummary;
import com.hireconnect.analyticsservice.dto.JobSummary;
import com.hireconnect.analyticsservice.dto.RecruiterAnalyticsResponse;
import com.hireconnect.analyticsservice.exception.AnalyticsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private UserClient userClient;

    @Mock
    private JobClient jobClient;

    @Mock
    private InterviewClient interviewClient;

    @Mock
    private SubscriptionClient subscriptionClient;

    @Mock
    private ApplicationClient applicationClient;

    @InjectMocks
    private AnalyticsService service;

    @Test
    void getDashboardDataCollectsCountsFromClients() {
        when(userClient.getUserCount()).thenReturn(10L);
        when(jobClient.getJobCount()).thenReturn(20L);
        when(applicationClient.getApplicationCount()).thenReturn(30L);
        when(interviewClient.getInterviewCount()).thenReturn(40L);
        when(subscriptionClient.getPremiumUserCount()).thenReturn(5L);

        AnalyticsResponse response = service.getDashboardData();

        assertThat(response.getTotalUsers()).isEqualTo(10L);
        assertThat(response.getTotalJobs()).isEqualTo(20L);
        assertThat(response.getTotalApplications()).isEqualTo(30L);
        assertThat(response.getTotalInterviews()).isEqualTo(40L);
        assertThat(response.getPremiumUsers()).isEqualTo(5L);
    }

    @Test
    void getDashboardDataWrapsClientFailures() {
        when(userClient.getUserCount()).thenThrow(new RuntimeException("down"));

        assertThatThrownBy(() -> service.getDashboardData())
                .isInstanceOf(AnalyticsException.class)
                .hasMessageContaining("Failed to fetch analytics data");
    }

    @Test
    void getRecruiterAnalyticsBuildsJobReportsAndAverages() {
        JobSummary first = job(1L, "Backend", 100L, LocalDateTime.now().minusDays(10));
        JobSummary second = job(2L, "Frontend", 0L, LocalDateTime.now().minusDays(5));
        ApplicationSummary offered = application(1L, "OFFERED", first.getPostedAt().plusDays(4));
        ApplicationSummary rejected = application(1L, "REJECTED", first.getPostedAt().plusDays(2));
        when(jobClient.getJobsByEmployer(9L)).thenReturn(List.of(first, second));
        when(applicationClient.getApplicationsByJob(1L)).thenReturn(List.of(offered, rejected));
        when(applicationClient.getApplicationsByJob(2L)).thenReturn(null);

        RecruiterAnalyticsResponse response = service.getRecruiterAnalytics(9L);

        assertThat(response.getRecruiterId()).isEqualTo(9L);
        assertThat(response.getTotalJobs()).isEqualTo(2L);
        assertThat(response.getTotalViews()).isEqualTo(100L);
        assertThat(response.getTotalApplications()).isEqualTo(2L);
        assertThat(response.getAverageViewToApplyRatio()).isEqualTo(1.0);
        assertThat(response.getAverageTimeToHireDays()).isEqualTo(4.0);
        assertThat(response.getJobs()).hasSize(2);
        assertThat(response.getJobs().get(0).getViewToApplyRatio()).isEqualTo(2.0);
        assertThat(response.getJobs().get(1).getViewToApplyRatio()).isZero();
    }

    @Test
    void getRecruiterAnalyticsHandlesNullJobsAsEmptyList() {
        when(jobClient.getJobsByEmployer(9L)).thenReturn(null);

        RecruiterAnalyticsResponse response = service.getRecruiterAnalytics(9L);

        assertThat(response.getTotalJobs()).isZero();
        assertThat(response.getTotalViews()).isZero();
        assertThat(response.getTotalApplications()).isZero();
        assertThat(response.getAverageViewToApplyRatio()).isZero();
        assertThat(response.getAverageTimeToHireDays()).isNull();
        assertThat(response.getJobs()).isEmpty();
    }

    @Test
    void getRecruiterAnalyticsWrapsClientFailures() {
        when(jobClient.getJobsByEmployer(9L)).thenThrow(new RuntimeException("down"));

        assertThatThrownBy(() -> service.getRecruiterAnalytics(9L))
                .isInstanceOf(AnalyticsException.class)
                .hasMessageContaining("recruiter analytics");
    }

    private JobSummary job(Long id, String title, Long views, LocalDateTime postedAt) {
        JobSummary job = new JobSummary();
        job.setId(id);
        job.setTitle(title);
        job.setStatus("OPEN");
        job.setViewCount(views);
        job.setPostedAt(postedAt);
        return job;
    }

    private ApplicationSummary application(Long jobId, String status, LocalDateTime appliedAt) {
        ApplicationSummary application = new ApplicationSummary();
        application.setJobId(jobId);
        application.setStatus(status);
        application.setAppliedAt(appliedAt);
        return application;
    }
}
