package com.hireconnect.auth.dto;

public class AuthResponse {

    private String token;
    private String message;
    private String name;
    private String email;
    private String role;
    private Long userId;

    public AuthResponse() {}

    public AuthResponse(String token, String message, String name, String email, String role, Long userId) {
        this.token = token;
        this.message = message;
        this.name = name;
        this.email = email;
        this.role = role;
        this.userId = userId;
    }

    // getters
    public String getToken() { return token; }
    public String getMessage() { return message; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public Long getUserId() { return userId; }
}
