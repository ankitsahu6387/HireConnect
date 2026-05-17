package com.hireconnect.analyticsservice.client;

import java.util.List;

import com.hireconnect.analyticsservice.dto.JobSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "job-service")
public interface JobClient {

    @GetMapping("/jobs/count")
    Long getJobCount();

    @GetMapping("/jobs/employer/{id}")
    List<JobSummary> getJobsByEmployer(@PathVariable("id") Long employerId);
}
