package com.hireconnect.subscriptionservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PaymentServiceTest {

    private final PaymentService service = new PaymentService();

    @Test
    void createOrderUsesDummyModeWhenKeysAreBlank() throws Exception {
        ReflectionTestUtils.setField(service, "key", "");
        ReflectionTestUtils.setField(service, "secret", "");

        JSONObject order = new JSONObject(service.createOrder(250));

        assertThat(order.getInt("amount")).isEqualTo(25000);
        assertThat(order.getString("currency")).isEqualTo("INR");
        assertThat(order.getString("status")).isEqualTo("created");
        assertThat(order.getString("mode")).isEqualTo("DUMMY");
    }

    @Test
    void getKeyReturnsConfiguredKey() {
        ReflectionTestUtils.setField(service, "key", "rzp_test_key");

        assertThat(service.getKey()).isEqualTo("rzp_test_key");
    }
}
