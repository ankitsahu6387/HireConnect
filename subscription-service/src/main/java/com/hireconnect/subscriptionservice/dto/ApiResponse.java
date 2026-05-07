package com.hireconnect.subscriptionservice.dto;

import java.time.LocalDateTime;

public class ApiResponse {

    private String message;
    private boolean success;
    private Object data;                // ✅ added
    private LocalDateTime timestamp;

    // Default Constructor
    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    // 2 Param Constructor
    public ApiResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
        this.timestamp = LocalDateTime.now();
    }

    // ✅ 3 Param Constructor (IMPORTANT)
    public ApiResponse(String message, boolean success, Object data) {
        this.message = message;
        this.success = success;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    // Getters & Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}