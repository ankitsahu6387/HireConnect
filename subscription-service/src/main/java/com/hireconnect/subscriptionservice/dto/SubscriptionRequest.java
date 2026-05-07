package com.hireconnect.subscriptionservice.dto;

public class SubscriptionRequest {

    private Long userId;
    private String plan;


    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }
}