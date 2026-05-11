package com.hireconnect.analyticsservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.hireconnect.analyticsservice.dto.*;
import com.hireconnect.analyticsservice.service.AnalyticsService;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService service;

    @GetMapping("/dashboard")
    public ApiResponse getDashboard() {

        AnalyticsResponse data = service.getDashboardData();

        return new ApiResponse("Dashboard fetched successfully", true, data);
    }

    @GetMapping("/platform")
    public ApiResponse getPlatformAnalytics() {

        AnalyticsResponse data = service.getDashboardData();

        return new ApiResponse("Platform analytics fetched successfully", true, data);
    }

    @GetMapping("/recruiter/{recruiterId}")
    public ApiResponse getRecruiterAnalytics(@PathVariable Long recruiterId) {

        RecruiterAnalyticsResponse data = service.getRecruiterAnalytics(recruiterId);

        return new ApiResponse("Recruiter analytics fetched successfully", true, data);
    }
}
