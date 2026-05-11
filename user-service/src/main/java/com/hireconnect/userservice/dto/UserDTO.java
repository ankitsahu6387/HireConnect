package com.hireconnect.userservice.dto;

public class UserDTO {

    private String name;
    private String email;
    private String role;
    private String skills;
    private String experience;
    private String company;
    private String resume;

    public UserDTO() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getResume() { return resume; }
    public void setResume(String resume) { this.resume = resume; }
}