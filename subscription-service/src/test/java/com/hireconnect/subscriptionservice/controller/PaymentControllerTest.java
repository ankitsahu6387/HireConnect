package com.hireconnect.subscriptionservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.hireconnect.subscriptionservice.dto.ApiResponse;
import com.hireconnect.subscriptionservice.entity.Subscription;
import com.hireconnect.subscriptionservice.service.PaymentService;
import com.hireconnect.subscriptionservice.service.SubscriptionService;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private SubscriptionService subscriptionService;

    private PaymentController controller;

    @BeforeEach
    void setUp() {
        controller = new PaymentController();
        ReflectionTestUtils.setField(controller, "paymentService", paymentService);
        ReflectionTestUtils.setField(controller, "subscriptionService", subscriptionService);
        ReflectionTestUtils.setField(controller, "razorpaySecret", "secret");
    }

    @Test
    void createOrderAndKeyDelegateToPaymentService() {
        when(paymentService.createOrder(500)).thenReturn("order-json");
        when(paymentService.getKey()).thenReturn("rzp_test");

        assertThat(controller.createOrder(500)).isEqualTo("order-json");
        ApiResponse response = controller.getPaymentKey();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo(Map.of("key", "rzp_test"));
    }

    @Test
    void verifyActivatesSubscriptionWhenSignatureIsAbsentOrValid() throws Exception {
        Subscription subscription = new Subscription();
        when(subscriptionService.activateSubscription(7L, "pay_dummy_success", "order_dummy_success", "PROFESSIONAL"))
                .thenReturn(subscription);
        when(subscriptionService.activateSubscription(7L, "pay_1", "order_1", "PREMIUM"))
                .thenReturn(subscription);

        ApiResponse fallback = controller.verify(Map.of("userId", "7"));
        assertThat(fallback.isSuccess()).isTrue();
        assertThat(fallback.getData()).isSameAs(subscription);

        String signature = signature("order_1", "pay_1");
        ApiResponse verified = controller.verify(Map.of(
                "userId", "7",
                "paymentId", "pay_1",
                "orderId", "order_1",
                "plan", "PREMIUM",
                "signature", signature));
        assertThat(verified.isSuccess()).isTrue();
        assertThat(verified.getMessage()).isEqualTo("Subscription activated");
    }

    @Test
    void verifyRejectsBadSignature() {
        ApiResponse response = controller.verify(Map.of(
                "userId", "7",
                "paymentId", "pay_1",
                "orderId", "order_1",
                "signature", "bad"));

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Payment signature verification failed");
    }

    private static String signature(String orderId, String paymentId) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8));
        StringBuilder actual = new StringBuilder();
        for (byte b : digest) {
            actual.append(String.format("%02x", b));
        }
        return actual.toString();
    }
}
