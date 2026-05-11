package com.hireconnect.analyticsservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "subscription-service", url = "http://localhost:8087")
public interface SubscriptionClient {

    @GetMapping("/subscription/premium/count")
    Long getPremiumUserCount();
}