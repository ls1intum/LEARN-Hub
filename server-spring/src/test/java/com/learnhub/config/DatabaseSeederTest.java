package com.learnhub.config;

import com.learnhub.activitymanagement.repository.ActivityRepository;
import com.learnhub.documentmanagement.repository.PDFDocumentRepository;
import com.learnhub.usermanagement.entity.User;
import com.learnhub.usermanagement.entity.enums.UserRole;
import com.learnhub.usermanagement.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatabaseSeederTest {

    @Test
    void createAdminUserUsesInitialAdminPasswordWhenSet() {
        DatabaseSeeder seeder = new DatabaseSeeder();
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        ReflectionTestUtils.setField(seeder, "userRepository", userRepository);
        ReflectionTestUtils.setField(seeder, "activityRepository", mock(ActivityRepository.class));
        ReflectionTestUtils.setField(seeder, "pdfDocumentRepository", mock(PDFDocumentRepository.class));
        ReflectionTestUtils.setField(seeder, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(seeder, "initialAdminEmail", "admin@learnhub.com");
        ReflectionTestUtils.setField(seeder, "initialAdminPassword", "seeded-admin-pwd");

        when(userRepository.existsByEmail("admin@learnhub.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        ReflectionTestUtils.invokeMethod(seeder, "createAdminUser");

        verify(passwordEncoder).encode("seeded-admin-pwd");
        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().getEmail()).isEqualTo("admin@learnhub.com");
        assertThat(savedUser.getValue().getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(savedUser.getValue().getPasswordHash()).isEqualTo("encoded");
    }

    @Test
    void createAdminUserGeneratesRandomPasswordWhenInitialPasswordBlank() {
        DatabaseSeeder seeder = new DatabaseSeeder();
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        ReflectionTestUtils.setField(seeder, "userRepository", userRepository);
        ReflectionTestUtils.setField(seeder, "activityRepository", mock(ActivityRepository.class));
        ReflectionTestUtils.setField(seeder, "pdfDocumentRepository", mock(PDFDocumentRepository.class));
        ReflectionTestUtils.setField(seeder, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(seeder, "initialAdminEmail", "admin@learnhub.com");
        ReflectionTestUtils.setField(seeder, "initialAdminPassword", " ");

        when(userRepository.existsByEmail("admin@learnhub.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        ReflectionTestUtils.invokeMethod(seeder, "createAdminUser");

        ArgumentCaptor<String> rawPassword = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).encode(rawPassword.capture());
        assertThat(rawPassword.getValue()).hasSize(12);
    }

    @Test
    void createAdminUserUsesInitialAdminEmailWhenSet() {
        DatabaseSeeder seeder = new DatabaseSeeder();
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        ReflectionTestUtils.setField(seeder, "userRepository", userRepository);
        ReflectionTestUtils.setField(seeder, "activityRepository", mock(ActivityRepository.class));
        ReflectionTestUtils.setField(seeder, "pdfDocumentRepository", mock(PDFDocumentRepository.class));
        ReflectionTestUtils.setField(seeder, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(seeder, "initialAdminEmail", "ops-admin@learnhub.com");
        ReflectionTestUtils.setField(seeder, "initialAdminPassword", "seeded-admin-pwd");

        when(userRepository.existsByEmail("ops-admin@learnhub.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        ReflectionTestUtils.invokeMethod(seeder, "createAdminUser");

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().getEmail()).isEqualTo("ops-admin@learnhub.com");
    }

    @Test
    void createAdminUserSkipsCreationWhenAdminAlreadyExists() {
        DatabaseSeeder seeder = new DatabaseSeeder();
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        ReflectionTestUtils.setField(seeder, "userRepository", userRepository);
        ReflectionTestUtils.setField(seeder, "activityRepository", mock(ActivityRepository.class));
        ReflectionTestUtils.setField(seeder, "pdfDocumentRepository", mock(PDFDocumentRepository.class));
        ReflectionTestUtils.setField(seeder, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(seeder, "initialAdminEmail", "admin@learnhub.com");
        ReflectionTestUtils.setField(seeder, "initialAdminPassword", "seeded-admin-pwd");

        when(userRepository.existsByEmail("admin@learnhub.com")).thenReturn(true);

        ReflectionTestUtils.invokeMethod(seeder, "createAdminUser");

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }
}
