package com.hireconnect.userservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.hireconnect.userservice.dto.UserDTO;
import com.hireconnect.userservice.entity.User;
import com.hireconnect.userservice.repository.UserRepository;
import com.hireconnect.userservice.service.UserService;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping("/{id}")
    public User createUser(@PathVariable Long id, @RequestBody UserDTO dto) {
        return service.createUser(id, dto);
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return service.getUserById(id);
    }

    @GetMapping
    public List<User> getAllUsers() {
        return service.getAllUsers();
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody UserDTO dto) {
        return service.updateUser(id, dto);
    }

    // Profile Endpoints matching the exact path asked
    @GetMapping("/profile/{id}")
    public User getProfile(@PathVariable Long id) {
        return service.getUserById(id);
    }

    @PutMapping("/profile/update/{id}")
    public User updateProfile(@PathVariable Long id, @RequestBody UserDTO dto) {
        return service.updateUser(id, dto);
    }

    @Autowired
    private UserRepository userRepository;
    
    @GetMapping("/count")
    public Long getUserCount() {
        return userRepository.count(); 
    }
}