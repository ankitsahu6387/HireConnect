package com.hireconnect.userservice.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.hireconnect.userservice.dto.UserDTO;
import com.hireconnect.userservice.entity.User;
import com.hireconnect.userservice.exception.DuplicateUserException;
import com.hireconnect.userservice.exception.ResourceNotFoundException;
import com.hireconnect.userservice.repository.UserRepository;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository repository;

    public UserServiceImpl(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public User createUser(Long id, UserDTO dto) {

        if (repository.findById(id).isPresent()) {
            return mergeUser(repository.findById(id).get(), dto);
        }

        if (repository.findByEmail(dto.getEmail()).isPresent()) {
            User existingUser = repository.findByEmail(dto.getEmail()).get();
            repository.delete(existingUser);
            repository.flush();
        }

        User user = new User(
                id,
                dto.getName(),
                dto.getEmail(),
                dto.getRole(),
                dto.getSkills(),
                dto.getExperience(),
                dto.getCompany(),
                dto.getResume());

        return repository.save(user);
    }

    private User mergeUser(User user, UserDTO dto) {
        user.setName(firstNonBlank(dto.getName(), user.getName()));
        user.setEmail(firstNonBlank(dto.getEmail(), user.getEmail()));
        user.setRole(firstNonBlank(dto.getRole(), user.getRole()));
        user.setSkills(firstNonBlank(dto.getSkills(), user.getSkills()));
        user.setExperience(firstNonBlank(dto.getExperience(), user.getExperience()));
        user.setCompany(firstNonBlank(dto.getCompany(), user.getCompany()));
        user.setResume(firstNonBlank(dto.getResume(), user.getResume()));
        return repository.save(user);
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    @Override
    public User getUserById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    public List<User> getAllUsers() {
        return repository.findAll();
    }

    @Override
    public User updateUser(Long id, UserDTO dto) {
        User user = getUserById(id);
       
        // Check duplicate email (if changed)
        if (!user.getEmail().equals(dto.getEmail()) &&
                repository.findByEmail(dto.getEmail()).isPresent()) {

            throw new DuplicateUserException("Email already in use");
        }

        user.setName(dto.getName());
        user.setSkills(dto.getSkills());
        user.setExperience(dto.getExperience());
        user.setCompany(dto.getCompany());
        user.setResume(dto.getResume());

        return repository.save(user);
    }
}
