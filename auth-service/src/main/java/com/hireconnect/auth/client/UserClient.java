package com.hireconnect.auth.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "user-service")
public interface UserClient {

    @PostMapping("/users/{id}")
    Object createUser(@PathVariable("id") Long id, @RequestBody Map<String, Object> dto);

    @GetMapping("/users/{id}")
    Object getUser(@PathVariable("id") Long id);
}
