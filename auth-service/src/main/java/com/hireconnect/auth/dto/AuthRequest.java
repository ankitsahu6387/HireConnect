package com.hireconnect.auth.dto;

public class AuthRequest {

    private String name;
    private String email;
    private String password;
    private String role;
    private String otp;

    public AuthRequest() {}

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public String getOtp() { return otp; }

    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setRole(String role) { this.role = role; }
    public void setOtp(String otp) { this.otp = otp; }
}
