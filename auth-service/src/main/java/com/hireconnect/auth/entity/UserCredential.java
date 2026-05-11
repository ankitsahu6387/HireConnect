package com.hireconnect.auth.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_credential")
public class UserCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String password;
    private boolean emailVerified;

    @Enumerated(EnumType.STRING)
    private Role role;

    public UserCredential() {}

    public UserCredential(String name, String email, String password, Role role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.emailVerified = false;
    }

    public UserCredential(String name, String email, String password, Role role, boolean emailVerified) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.emailVerified = emailVerified;
    }

    // GETTERS & SETTERS

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
