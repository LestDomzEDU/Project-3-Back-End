package com.project03.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.project03.model.StudentPreference;
import com.project03.model.User;
import com.project03.repository.StudentPreferenceRepository;
import com.project03.repository.UserRepository;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class StudentPreferenceControllerUnitTest {

    @Test
    @DisplayName("savePreferences returns 404 when user not found")
    void savePreferences_userNotFound() {
        StudentPreferenceRepository prefRepo = mock(StudentPreferenceRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        StudentPreferenceController controller =
                new StudentPreferenceController(prefRepo, userRepo);

        when(userRepo.findById(1L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.savePreferences(1L, new StudentPreference());

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        verify(prefRepo, never()).save(any());
    }

    @Test
    @DisplayName("savePreferences updates existing preferences")
    void savePreferences_updatesExisting() {
        StudentPreferenceRepository prefRepo = mock(StudentPreferenceRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        StudentPreferenceController controller =
                new StudentPreferenceController(prefRepo, userRepo);

        User user = new User();
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));

        StudentPreference existing = new StudentPreference();
        existing.setState("CA");
        when(prefRepo.findByUser(user)).thenReturn(Optional.of(existing));
        when(prefRepo.save(any(StudentPreference.class))).thenAnswer(inv -> inv.getArgument(0));

        StudentPreference incoming = new StudentPreference();
        incoming.setState("NY");

        ResponseEntity<?> response = controller.savePreferences(1L, incoming);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        StudentPreference saved = (StudentPreference) response.getBody();
        assertThat(saved.getState()).isEqualTo("NY");
        verify(prefRepo).save(existing);
    }

    @Test
    @DisplayName("savePreferences creates new preferences if none exist")
    void savePreferences_createsNew() {
        StudentPreferenceRepository prefRepo = mock(StudentPreferenceRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        StudentPreferenceController controller =
                new StudentPreferenceController(prefRepo, userRepo);

        User user = new User();
        when(userRepo.findById(2L)).thenReturn(Optional.of(user));
        when(prefRepo.findByUser(user)).thenReturn(Optional.empty());
        when(prefRepo.save(any(StudentPreference.class))).thenAnswer(inv -> inv.getArgument(0));

        StudentPreference incoming = new StudentPreference();
        incoming.setState("TX");

        ResponseEntity<?> response = controller.savePreferences(2L, incoming);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        StudentPreference saved = (StudentPreference) response.getBody();
        assertThat(saved.getState()).isEqualTo("TX");
        verify(prefRepo).save(any(StudentPreference.class));
    }

    @Test
    @DisplayName("getPreferences returns 404 when user missing")
    void getPreferences_userMissing() {
        StudentPreferenceRepository prefRepo = mock(StudentPreferenceRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        StudentPreferenceController controller =
                new StudentPreferenceController(prefRepo, userRepo);

        when(userRepo.findById(1L)).thenReturn(Optional.empty());

        ResponseEntity<StudentPreference> response = controller.getPreferences(1L);
        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @DisplayName("getPreferences returns 404 if user has no preferences")
    void getPreferences_noPrefs() {
        StudentPreferenceRepository prefRepo = mock(StudentPreferenceRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        StudentPreferenceController controller =
                new StudentPreferenceController(prefRepo, userRepo);

        User user = new User();
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(prefRepo.findByUser(user)).thenReturn(Optional.empty());

        ResponseEntity<StudentPreference> response = controller.getPreferences(1L);
        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @DisplayName("getPreferences returns existing preferences")
    void getPreferences_ok() {
        StudentPreferenceRepository prefRepo = mock(StudentPreferenceRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        StudentPreferenceController controller =
                new StudentPreferenceController(prefRepo, userRepo);

        User user = new User();
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        StudentPreference pref = new StudentPreference();
        when(prefRepo.findByUser(user)).thenReturn(Optional.of(pref));

        ResponseEntity<StudentPreference> response = controller.getPreferences(1L);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isSameAs(pref);
    }

    @Test
    @DisplayName("updatePreferences returns 404 if user or preference not found")
    void updatePreferences_notFound() {
        StudentPreferenceRepository prefRepo = mock(StudentPreferenceRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        StudentPreferenceController controller =
                new StudentPreferenceController(prefRepo, userRepo);

        when(userRepo.findById(1L)).thenReturn(Optional.empty());
        ResponseEntity<StudentPreference> r1 =
                controller.updatePreferences(1L, new StudentPreference());
        assertThat(r1.getStatusCode().value()).isEqualTo(404);

        User user = new User();
        when(userRepo.findById(2L)).thenReturn(Optional.of(user));
        when(prefRepo.findByUser(user)).thenReturn(Optional.empty());

        ResponseEntity<StudentPreference> r2 =
                controller.updatePreferences(2L, new StudentPreference());
        assertThat(r2.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @DisplayName("deletePreferences handles user missing / prefs missing / success")
    void deletePreferences_variants() {
        StudentPreferenceRepository prefRepo = mock(StudentPreferenceRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        StudentPreferenceController controller =
                new StudentPreferenceController(prefRepo, userRepo);

        // user not found
        when(userRepo.findById(1L)).thenReturn(Optional.empty());
        ResponseEntity<Void> r1 = controller.deletePreferences(1L);
        assertThat(r1.getStatusCode().value()).isEqualTo(404);

        // user found, no prefs
        User user = new User();
        when(userRepo.findById(2L)).thenReturn(Optional.of(user));
        when(prefRepo.existsByUser(user)).thenReturn(false);
        ResponseEntity<Void> r2 = controller.deletePreferences(2L);
        assertThat(r2.getStatusCode().value()).isEqualTo(404);

        // user + prefs exist
        when(userRepo.findById(3L)).thenReturn(Optional.of(user));
        when(prefRepo.existsByUser(user)).thenReturn(true);
        ResponseEntity<Void> r3 = controller.deletePreferences(3L);
        assertThat(r3.getStatusCode().is2xxSuccessful()).isTrue();
        verify(prefRepo).deleteByUser(user);
    }
}

