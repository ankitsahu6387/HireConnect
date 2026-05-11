package com.hireconnect.analyticsservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "interview-service", url = "http://localhost:8085")
public interface InterviewClient {

    @GetMapping("/interviews/count")
    Long getInterviewCount();
}