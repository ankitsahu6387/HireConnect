package com.hireconnect.subscriptionservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import com.hireconnect.subscriptionservice.dto.SubscriptionRequest;
import com.hireconnect.subscriptionservice.entity.Subscription;
import com.hireconnect.subscriptionservice.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository repository;

    @InjectMocks
    private SubscriptionService service;

    @Test
    void createOrUpdateSubscriptionCreatesPendingSubscription() {
        SubscriptionRequest request = new SubscriptionRequest();
        request.setUserId(5L);
        request.setPlan("premium");
        when(repository.findByUserId(5L)).thenReturn(null);

        service.createOrUpdateSubscription(request);

        verify(repository).save(any(Subscription.class));
    }

    @Test
    void activateSubscriptionCreatesActiveMonthlySubscription() {
        when(repository.findByUserId(5L)).thenReturn(null);
        when(repository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Subscription result = service.activateSubscription(5L, "pay_1", "order_1", "enterprise");

        assertThat(result.getUserId()).isEqualTo(5L);
        assertThat(result.getPlan()).isEqualTo("ENTERPRISE");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getStartDate()).isEqualTo(LocalDate.now());
        assertThat(result.getEndDate()).isEqualTo(LocalDate.now().plusMonths(1));
    }

    @Test
    void getSubscriptionReturnsFreeInactiveWhenMissing() {
        when(repository.findByUserId(5L)).thenReturn(null);

        Subscription result = service.getSubscription(5L);

        assertThat(result.getPlan()).isEqualTo("FREE");
        assertThat(result.getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    void getSubscriptionExpiresOldActiveSubscription() {
        Subscription subscription = new Subscription();
        subscription.setUserId(5L);
        subscription.setStatus("ACTIVE");
        subscription.setEndDate(LocalDate.now().minusDays(1));
        when(repository.findByUserId(5L)).thenReturn(subscription);
        when(repository.save(subscription)).thenReturn(subscription);

        Subscription result = service.getSubscription(5L);

        assertThat(result.getStatus()).isEqualTo("EXPIRED");
        verify(repository).save(subscription);
    }

    @Test
    void cancelSubscriptionMarksExistingSubscriptionCancelled() {
        Subscription subscription = new Subscription();
        when(repository.findByUserId(5L)).thenReturn(subscription);

        service.cancelSubscription(5L);

        assertThat(subscription.getStatus()).isEqualTo("CANCELLED");
        verify(repository).save(subscription);
    }

    @Test
    void cancelSubscriptionThrowsWhenMissing() {
        when(repository.findByUserId(5L)).thenReturn(null);

        assertThatThrownBy(() -> service.cancelSubscription(5L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void getPremiumUserCountCountsProfessionalAndEnterprise() {
        when(repository.countByPlanAndStatus("PROFESSIONAL", "ACTIVE")).thenReturn(3L);
        when(repository.countByPlanAndStatus("ENTERPRISE", "ACTIVE")).thenReturn(2L);

        assertThat(service.getPremiumUserCount()).isEqualTo(5L);
    }
}
