package com.hireconnect.subscriptionservice.controller;

import com.hireconnect.subscriptionservice.dto.ApiResponse;
import com.hireconnect.subscriptionservice.entity.Subscription;
import com.hireconnect.subscriptionservice.service.PaymentService;
import com.hireconnect.subscriptionservice.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Value("${razorpay.secret}")
    private String razorpaySecret;

    @PostMapping("/create")
    public String createOrder(@RequestParam int amount) {
        return paymentService.createOrder(amount);
    }

    @GetMapping("/key")
    public ApiResponse getPaymentKey() {
        return new ApiResponse("Razorpay key fetched", true, Map.of("key", paymentService.getKey()));
    }

    @PostMapping("/verify")
    public ApiResponse verify(@RequestBody Map<String, String> data) {
        Long userId = Long.parseLong(data.get("userId"));
        String paymentId = data.getOrDefault("paymentId", "pay_dummy_success");
        String orderId = data.getOrDefault("orderId", "order_dummy_success");
        String plan = data.getOrDefault("plan", "PROFESSIONAL");
        String signature = data.get("signature");

        if (signature != null && !signature.isBlank() && !isValidSignature(orderId, paymentId, signature)) {
            return new ApiResponse("Payment signature verification failed", false, null);
        }

        Subscription subscription = subscriptionService.activateSubscription(userId, paymentId, orderId, plan);
        return new ApiResponse("Subscription activated", true, subscription);
    }

    private boolean isValidSignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpaySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder actual = new StringBuilder();
            for (byte b : digest) {
                actual.append(String.format("%02x", b));
            }
            return actual.toString().equals(signature);
        } catch (Exception e) {
            return false;
        }
    }
}
