package com.hireconnect.subscriptionservice.service;

import com.hireconnect.subscriptionservice.dto.SubscriptionRequest;
import com.hireconnect.subscriptionservice.entity.Subscription;
import com.hireconnect.subscriptionservice.repository.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class SubscriptionService {

    @Autowired
    private SubscriptionRepository repository;

    public void createOrUpdateSubscription(SubscriptionRequest request) {
        Subscription sub = repository.findByUserId(request.getUserId());

        if (sub == null) {
            sub = new Subscription();
            sub.setUserId(request.getUserId());
        }

        sub.setPlan(normalizePlan(request.getPlan()));
        sub.setStatus("PENDING");
        repository.save(sub);
    }

    public Subscription activateSubscription(Long userId, String paymentId, String orderId) {
        return activateSubscription(userId, paymentId, orderId, "PROFESSIONAL");
    }

    public Subscription activateSubscription(Long userId, String paymentId, String orderId, String plan) {
        Subscription sub = repository.findByUserId(userId);

        if (sub == null) {
            sub = new Subscription();
            sub.setUserId(userId);
        }

        sub.setPlan(normalizePlan(plan));
        sub.setStatus("ACTIVE");
        sub.setPaymentId(paymentId);
        sub.setOrderId(orderId);
        LocalDate startDate = LocalDate.now();
        sub.setStartDate(startDate);
        sub.setEndDate(startDate.plusMonths(1));

        return repository.save(sub);
    }

    public Subscription getSubscription(Long userId) {
        Subscription sub = repository.findByUserId(userId);

        if (sub == null) {
            Subscription free = new Subscription();
            free.setUserId(userId);
            free.setPlan("FREE");
            free.setStatus("INACTIVE");
            return free;
        }

        return expireIfNeeded(sub);
    }

    public void cancelSubscription(Long userId) {
        Subscription sub = repository.findByUserId(userId);

        if (sub == null) {
            throw new RuntimeException("Subscription not found");
        }

        sub.setStatus("CANCELLED");
        repository.save(sub);
    }

    public Long getPremiumUserCount() {
        return repository.countByPlanAndStatus("PROFESSIONAL", "ACTIVE")
                + repository.countByPlanAndStatus("ENTERPRISE", "ACTIVE");
    }

    private Subscription expireIfNeeded(Subscription sub) {
        if ("ACTIVE".equals(sub.getStatus())
                && sub.getEndDate() != null
                && sub.getEndDate().isBefore(LocalDate.now())) {
            sub.setStatus("EXPIRED");
            return repository.save(sub);
        }

        return sub;
    }

    private String normalizePlan(String plan) {
        if (plan == null || plan.isBlank()) {
            return "PROFESSIONAL";
        }

        String normalized = plan.trim().toUpperCase();
        if (normalized.equals("PREMIUM")) {
            return "PROFESSIONAL";
        }

        if (!normalized.equals("FREE") && !normalized.equals("PROFESSIONAL") && !normalized.equals("ENTERPRISE")) {
            return "PROFESSIONAL";
        }

        return normalized;
    }
}
