package com.hireconnect.userservice.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    private Long id; // Same ID from Auth Service

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String email;

    private String role; // JOB_SEEKER / EMPLOYER

    private String skills;
    private String experience;
    private String company;
    private String resume;

    public User() {}

    public User(Long id, String name, String email, String role,
                String skills, String experience, String company, String resume) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.skills = skills;
        this.experience = experience;
        this.company = company;
        this.resume = resume;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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