package com.hireconnect.userservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.hireconnect.userservice.dto.UserDTO;
import com.hireconnect.userservice.entity.User;
import com.hireconnect.userservice.exception.DuplicateUserException;
import com.hireconnect.userservice.exception.ResourceNotFoundException;
import com.hireconnect.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private UserServiceImpl service;

    @Test
    void createUserCreatesNewRecord() {
        UserDTO dto = dto();
        when(repository.findById(1L)).thenReturn(Optional.empty());
        when(repository.findByEmail("a@b.com")).thenReturn(Optional.empty());
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = service.createUser(1L, dto);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Ankit");
        assertThat(result.getEmail()).isEqualTo("a@b.com");
    }

    @Test
    void createUserMergesWhenIdAlreadyExists() {
        User existing = new User(1L, "Old", "old@b.com", "CANDIDATE", "Java", "1", null, null);
        UserDTO dto = dto();
        dto.setSkills("");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        User result = service.createUser(1L, dto);

        assertThat(result.getName()).isEqualTo("Ankit");
        assertThat(result.getSkills()).isEqualTo("Java");
        verify(repository).save(existing);
    }

    @Test
    void createUserDeletesExistingEmailOwnerBeforeSaving() {
        User duplicate = new User(2L, "Other", "a@b.com", "EMPLOYER", null, null, null, null);
        when(repository.findById(1L)).thenReturn(Optional.empty());
        when(repository.findByEmail("a@b.com")).thenReturn(Optional.of(duplicate));
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createUser(1L, dto());

        verify(repository).delete(duplicate);
        verify(repository).flush();
    }

    @Test
    void updateUserRejectsDuplicateEmail() {
        User existing = new User(1L, "Ankit", "a@b.com", "CANDIDATE", null, null, null, null);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        UserDTO dto = dto();
        dto.setEmail("other@b.com");
        when(repository.findByEmail("other@b.com")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> service.updateUser(1L, dto))
                .isInstanceOf(DuplicateUserException.class);
    }

    @Test
    void updateUserSavesProfileFields() {
        User existing = new User(1L, "Old", "a@b.com", "CANDIDATE", null, null, null, null);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        User result = service.updateUser(1L, dto());

        assertThat(result.getName()).isEqualTo("Ankit");
        assertThat(result.getCompany()).isEqualTo("HireConnect");
    }

    @Test
    void getUserByIdThrowsWhenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllUsersDelegatesToRepository() {
        User user = new User(1L, "Ankit", "a@b.com", "CANDIDATE", null, null, null, null);
        when(repository.findAll()).thenReturn(List.of(user));

        assertThat(service.getAllUsers()).containsExactly(user);
    }

    private UserDTO dto() {
        UserDTO dto = new UserDTO();
        dto.setName("Ankit");
        dto.setEmail("a@b.com");
        dto.setRole("CANDIDATE");
        dto.setSkills("Java");
        dto.setExperience("3");
        dto.setCompany("HireConnect");
        dto.setResume("resume.pdf");
        return dto;
    }
}
