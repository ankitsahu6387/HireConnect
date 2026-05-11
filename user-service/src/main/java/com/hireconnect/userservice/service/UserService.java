package com.hireconnect.userservice.service;

import java.util.List;
import com.hireconnect.userservice.entity.User;
import com.hireconnect.userservice.dto.UserDTO;

public interface UserService {

    User createUser(Long id, UserDTO dto);

    User getUserById(Long id);

    List<User> getAllUsers();

    User updateUser(Long id, UserDTO dto);
}