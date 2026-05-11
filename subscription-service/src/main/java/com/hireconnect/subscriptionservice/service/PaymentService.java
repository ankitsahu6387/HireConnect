package com.hireconnect.subscriptionservice.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class PaymentService {

    @Value("${razorpay.key:}")
    private String key;

    @Value("${razorpay.secret:}")
    private String secret;

    public String createOrder(int amount) {
        try {
            if (isDummyMode()) {
                return dummyOrder(amount, "DUMMY");
            }

            RazorpayClient client = new RazorpayClient(key, secret);

            JSONObject options = new JSONObject();
            options.put("amount", amount * 100);
            options.put("currency", "INR");

            Order order = client.orders.create(options);
            return order.toString();
        } catch (Exception e) {
            return dummyOrder(amount, "DUMMY_FALLBACK");
        }
    }

    public String getKey() {
        return key;
    }

    private String dummyOrder(int amount, String mode) {
        JSONObject order = new JSONObject();
        order.put("id", "order_dummy_" + Instant.now().toEpochMilli());
        order.put("amount", amount * 100);
        order.put("currency", "INR");
        order.put("status", "created");
        order.put("mode", mode);
        return order.toString();
    }

    private boolean isDummyMode() {
        return key == null || secret == null
                || key.isBlank() || secret.isBlank()
                || key.contains("xxxxx") || secret.contains("xxxx");
    }
}
