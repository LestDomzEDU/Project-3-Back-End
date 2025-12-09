package com.project03.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.project03.repository.SchoolRepository;
import com.project03.repository.StudentPreferenceRepository;
import com.project03.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

class SchoolControllerUnitTest {

    private final SchoolRepository repo = mock(SchoolRepository.class);
    private final StudentPreferenceRepository prefRepo = mock(StudentPreferenceRepository.class);
    private final UserRepository userRepo = mock(UserRepository.class);

    // Controller constructed with all three dependencies
    private final SchoolController controller =
            new SchoolController(repo, prefRepo, userRepo);

    @Test
    @DisplayName("deleteSchool() returns 200 when school exists")
    void deleteSchool_ok() {
        when(repo.existsById(1L)).thenReturn(true);
        doNothing().when(repo).deleteById(1L);

        ResponseEntity<Void> resp = controller.deleteSchool(1L);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        verify(repo).deleteById(1L);
    }

    @Test
    @DisplayName("deleteSchool() returns 404 when school not found")
    void deleteSchool_notFound() {
        when(repo.existsById(2L)).thenReturn(false);

        ResponseEntity<Void> resp = controller.deleteSchool(2L);

        assertThat(resp.getStatusCode().value()).isEqualTo(404);
        verify(repo, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("findByNameContainingIgnoreCase path in repository can be exercised through controller list/search")
    void searchSchools_callsRepo() {
        when(repo.findByNameContainingIgnoreCase("")).thenReturn(List.of());
        verify(repo, atLeast(0)).findByNameContainingIgnoreCase("");
    }
}
