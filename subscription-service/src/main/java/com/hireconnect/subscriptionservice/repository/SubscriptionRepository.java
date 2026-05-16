package com.hireconnect.subscriptionservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hireconnect.subscriptionservice.entity.Subscription;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Subscription findByUserId(Long userId);

    Long countByPlan(String plan);

    Long countByPlanAndStatus(String plan, String status);
}
