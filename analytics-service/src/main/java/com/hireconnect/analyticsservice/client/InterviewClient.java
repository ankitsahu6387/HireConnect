package com.hireconnect.analyticsservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "interview-service")
public interface InterviewClient {

    @GetMapping("/interviews/count")
    Long getInterviewCount();
}
