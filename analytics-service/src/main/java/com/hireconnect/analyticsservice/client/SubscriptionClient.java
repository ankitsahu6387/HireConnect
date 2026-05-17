package com.hireconnect.analyticsservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "subscription-service")
public interface SubscriptionClient {

    @GetMapping("/subscription/premium/count")
    Long getPremiumUserCount();
}
