package com.hireconnect.analyticsservice.client;

import java.util.List;

import com.hireconnect.analyticsservice.dto.ApplicationSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "application-service")
public interface ApplicationClient {

    @GetMapping("/applications/count")
    Long getApplicationCount();

    @GetMapping("/applications/job/{id}")
    List<ApplicationSummary> getApplicationsByJob(@PathVariable("id") Long jobId);
}
