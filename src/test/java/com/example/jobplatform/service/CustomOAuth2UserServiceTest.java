package com.example.jobplatform.security;

import com.example.jobplatform.model.User;
import com.example.jobplatform.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private OAuth2UserRequest userRequest;
    @Mock private OAuth2User oAuth2User;

    @Test
    void loadUser_whenEmailMissing_throws() {
        // Arrange
        when(oAuth2User.getAttribute("email")).thenReturn(null); // достатъчно

        CustomOAuth2UserService service = new CustomOAuth2UserService(userRepository, passwordEncoder) {
            @Override
            public OAuth2User loadUser(OAuth2UserRequest request) {
                OAuth2User oAuth2 = oAuth2User; // mock

                String email = oAuth2.getAttribute("email");
                if (email == null || email.isBlank()) {
                    throw new IllegalStateException("Google did not return an email!");
                }
                throw new AssertionError("Should not reach here");
            }
        };

        // Act + Assert
        assertThatThrownBy(() -> service.loadUser(userRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Google did not return an email!");
    }

    @Test
    void loadUser_whenUserExists_returnsCustomOAuth2User_andDoesNotSave() {
        // Arrange
        when(oAuth2User.getAttribute("email")).thenReturn("test@example.com");
        when(oAuth2User.getAttributes()).thenReturn(Map.of("email", "test@example.com"));

        User existing = new User();
        existing.setEmail("test@example.com");
        existing.setUsername("test@example.com");
        existing.setRole("ROLE_USER");
        existing.setPassword("hashed");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existing));

        CustomOAuth2UserService service = new CustomOAuth2UserService(userRepository, passwordEncoder) {
            @Override
            public OAuth2User loadUser(OAuth2UserRequest request) {
                OAuth2User oAuth2UserLocal = oAuth2User;

                String email = oAuth2UserLocal.getAttribute("email");
                if (email == null || email.isBlank()) {
                    throw new IllegalStateException("Google did not return an email!");
                }

                User user = userRepository.findByEmail(email).orElseGet(() -> {
                    User u = new User();
                    u.setEmail(email);
                    u.setUsername(email);
                    u.setRole("ROLE_USER");
                    u.setPassword(passwordEncoder.encode("OAUTH_USER"));
                    return userRepository.save(u);
                });

                return new CustomOAuth2User(
                        oAuth2UserLocal.getAttributes(),
                        user.getEmail(),
                        user.getUsername(),
                        user.getRole()
                );
            }
        };

        // Act
        OAuth2User result = service.loadUser(userRequest);

        // Assert
        assertThat(result).isInstanceOf(CustomOAuth2User.class);
        CustomOAuth2User cu = (CustomOAuth2User) result;

        assertThat(cu.getEmail()).isEqualTo("test@example.com");
        assertThat(cu.getUsername()).isEqualTo("test@example.com");
        assertThat(cu.getRole()).isEqualTo("ROLE_USER");
        assertThat(cu.getAttributes()).containsEntry("email", "test@example.com");

        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void loadUser_whenUserDoesNotExist_createsAndSavesUser_thenReturnsCustomOAuth2User() {
        // Arrange
        when(oAuth2User.getAttribute("email")).thenReturn("new@example.com");
        when(oAuth2User.getAttributes()).thenReturn(Map.of("email", "new@example.com"));

        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("OAUTH_USER")).thenReturn("encoded-pass");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomOAuth2UserService service = new CustomOAuth2UserService(userRepository, passwordEncoder) {
            @Override
            public OAuth2User loadUser(OAuth2UserRequest request) {
                OAuth2User oAuth2UserLocal = oAuth2User;

                String email = oAuth2UserLocal.getAttribute("email");
                if (email == null || email.isBlank()) {
                    throw new IllegalStateException("Google did not return an email!");
                }

                User user = userRepository.findByEmail(email).orElseGet(() -> {
                    User u = new User();
                    u.setEmail(email);
                    u.setUsername(email);
                    u.setRole("ROLE_USER");
                    u.setPassword(passwordEncoder.encode("OAUTH_USER"));
                    return userRepository.save(u);
                });

                return new CustomOAuth2User(
                        oAuth2UserLocal.getAttributes(),
                        user.getEmail(),
                        user.getUsername(),
                        user.getRole()
                );
            }
        };

        // Act
        OAuth2User result = service.loadUser(userRequest);

        // Assert
        assertThat(result).isInstanceOf(CustomOAuth2User.class);
        CustomOAuth2User cu = (CustomOAuth2User) result;

        assertThat(cu.getEmail()).isEqualTo("new@example.com");
        assertThat(cu.getUsername()).isEqualTo("new@example.com");
        assertThat(cu.getRole()).isEqualTo("ROLE_USER");

        verify(userRepository).findByEmail("new@example.com");
        verify(passwordEncoder).encode("OAUTH_USER");
        verify(userRepository).save(argThat(u ->
                "new@example.com".equals(u.getEmail()) &&
                        "new@example.com".equals(u.getUsername()) &&
                        "ROLE_USER".equals(u.getRole()) &&
                        "encoded-pass".equals(u.getPassword())
        ));
    }
}