package com.example.jobplatform.service;

import com.example.jobplatform.model.User;
import com.example.jobplatform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserService userService;

    private User existing;

    @BeforeEach
    void setUp() {
        existing = new User();
        existing.setId(42L);
        existing.setUsername("alice");
        existing.setEmail("old@example.com");
        existing.setPassword("oldHash");
    }

    @Test
    void updateProfile_changesEmail_andHashesPassword_whenProvided() {
        when(userRepository.findById(42L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("newpass")).thenReturn("encNew");
        when(userRepository.save(existing)).thenReturn(existing);

        User updated = userService.updateProfile(42L, "new@example.com", "newpass");

        assertThat(updated.getEmail()).isEqualTo("new@example.com");
        assertThat(updated.getPassword()).isEqualTo("encNew");
        verify(passwordEncoder).encode("newpass");
        verify(userRepository).save(existing);
    }

    @Test
    void updateProfile_changesOnlyEmail_whenPasswordBlank() {
        when(userRepository.findById(42L)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        User updated = userService.updateProfile(42L, "only@example.com", "   ");

        assertThat(updated.getEmail()).isEqualTo("only@example.com");
        assertThat(updated.getPassword()).isEqualTo("oldHash");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository).save(existing);
    }

    @Test
    void updateProfile_throwsWhenUserMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile(99L, "x@x.com", "p"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found with ID: 99");
    }
}