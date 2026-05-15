package com.hireconnect.subscriptionservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.hireconnect.subscriptionservice.dto.ApiResponse;
import com.hireconnect.subscriptionservice.dto.SubscriptionRequest;
import com.hireconnect.subscriptionservice.entity.Subscription;
import com.hireconnect.subscriptionservice.repository.SubscriptionRepository;
import com.hireconnect.subscriptionservice.service.SubscriptionService;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    @Mock
    private SubscriptionService service;

    @Mock
    private SubscriptionRepository repository;

    private SubscriptionController controller;

    @BeforeEach
    void setUp() {
        controller = new SubscriptionController();
        ReflectionTestUtils.setField(controller, "service", service);
        ReflectionTestUtils.setField(controller, "repository", repository);
    }

    @Test
    void createDelegatesToService() {
        SubscriptionRequest request = new SubscriptionRequest();

        ApiResponse response = controller.create(request);

        verify(service).createOrUpdateSubscription(request);
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Subscription updated successfully");
    }

    @Test
    void getAndCountDelegateToService() {
        Subscription subscription = new Subscription();
        when(service.getSubscription(4L)).thenReturn(subscription);
        when(service.getPremiumUserCount()).thenReturn(8L);

        ApiResponse response = controller.get(4L);
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isSameAs(subscription);
        assertThat(controller.getPremiumCount()).isEqualTo(8L);
    }
}
