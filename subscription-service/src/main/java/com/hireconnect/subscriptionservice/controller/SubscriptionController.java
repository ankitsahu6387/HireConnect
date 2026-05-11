package com.hireconnect.subscriptionservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.hireconnect.subscriptionservice.dto.ApiResponse;
import com.hireconnect.subscriptionservice.dto.SubscriptionRequest;
import com.hireconnect.subscriptionservice.entity.Subscription;
import com.hireconnect.subscriptionservice.repository.SubscriptionRepository;
import com.hireconnect.subscriptionservice.service.SubscriptionService;

@RestController
@RequestMapping("/subscription")
public class SubscriptionController {

    @Autowired
    private SubscriptionService service;

    @Autowired
    private SubscriptionRepository repository;

    // CREATE
    @PostMapping("/create")
    public ApiResponse create(@RequestBody SubscriptionRequest request) {

        service.createOrUpdateSubscription(request);

        return new ApiResponse("Subscription updated successfully", true, null);
    }

    // GET
    @GetMapping("/{userId}")
    public ApiResponse get(@PathVariable Long userId) {

        Subscription sub = service.getSubscription(userId);

        return new ApiResponse("Subscription fetched successfully", true, sub);
    }

    // COUNT
    @GetMapping("/premium/count")
    public Long getPremiumCount() {
        return service.getPremiumUserCount();
    }
}
