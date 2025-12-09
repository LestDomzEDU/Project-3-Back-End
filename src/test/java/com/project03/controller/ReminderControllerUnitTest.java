package com.project03.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.project03.model.Reminder;
import com.project03.model.School;
import com.project03.model.User;
import com.project03.repository.ReminderRepository;
import com.project03.repository.SchoolRepository;
import com.project03.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class ReminderControllerUnitTest {

    private ReminderController makeController(
            ReminderRepository r, UserRepository u, SchoolRepository s) {
        return new ReminderController(r, u, s);
    }

    @Test
    @DisplayName("createReminder returns 404 when user not found")
    void createReminder_userNotFound() {
        ReminderRepository rRepo = mock(ReminderRepository.class);
        UserRepository uRepo = mock(UserRepository.class);
        SchoolRepository sRepo = mock(SchoolRepository.class);

        ReminderController controller = makeController(rRepo, uRepo, sRepo);

        when(uRepo.findById(1L)).thenReturn(Optional.empty());

        ResponseEntity<?> resp = controller.createReminder(1L, 2L);
        assertThat(resp.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @DisplayName("createReminder returns 404 when school not found")
    void createReminder_schoolNotFound() {
        ReminderRepository rRepo = mock(ReminderRepository.class);
        UserRepository uRepo = mock(UserRepository.class);
        SchoolRepository sRepo = mock(SchoolRepository.class);

        ReminderController controller = makeController(rRepo, uRepo, sRepo);

        when(uRepo.findById(1L)).thenReturn(Optional.of(new User()));
        when(sRepo.findById(2L)).thenReturn(Optional.empty());

        ResponseEntity<?> resp = controller.createReminder(1L, 2L);
        assertThat(resp.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @DisplayName("createReminder returns existing reminder when one already exists")
    void createReminder_existingReturned() {
        ReminderRepository rRepo = mock(ReminderRepository.class);
        UserRepository uRepo = mock(UserRepository.class);
        SchoolRepository sRepo = mock(SchoolRepository.class);

        ReminderController controller = makeController(rRepo, uRepo, sRepo);

        User user = new User();
        School school = new School();
        school.setApplicationDeadline(LocalDate.now().toString());
        when(uRepo.findById(1L)).thenReturn(Optional.of(user));
        when(sRepo.findById(2L)).thenReturn(Optional.of(school));

        Reminder existing = new Reminder();
        when(rRepo.findByUserAndSchoolId(user, 2L)).thenReturn(List.of(existing));

        ResponseEntity<?> resp = controller.createReminder(1L, 2L);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isSameAs(existing);
        verify(rRepo, never()).save(any());
    }

    @Test
    @DisplayName("createReminder fails when school has no deadline")
    void createReminder_missingDeadline() {
        ReminderRepository rRepo = mock(ReminderRepository.class);
        UserRepository uRepo = mock(UserRepository.class);
        SchoolRepository sRepo = mock(SchoolRepository.class);

        ReminderController controller = makeController(rRepo, uRepo, sRepo);

        User user = new User();
        School school = new School();
        school.setApplicationDeadline(null);

        when(uRepo.findById(1L)).thenReturn(Optional.of(user));
        when(sRepo.findById(2L)).thenReturn(Optional.of(school));
        when(rRepo.findByUserAndSchoolId(user, 2L)).thenReturn(List.of());

        ResponseEntity<?> resp = controller.createReminder(1L, 2L);
        assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) resp.getBody();
        assertThat(body.get("error")).contains("School does not have an application deadline");
    }

    @Test
    @DisplayName("createReminder fails on invalid date format")
    void createReminder_invalidDate() {
        ReminderRepository rRepo = mock(ReminderRepository.class);
        UserRepository uRepo = mock(UserRepository.class);
        SchoolRepository sRepo = mock(SchoolRepository.class);

        ReminderController controller = makeController(rRepo, uRepo, sRepo);

        User user = new User();
        School school = new School();
        school.setApplicationDeadline("not-a-date");

        when(uRepo.findById(1L)).thenReturn(Optional.of(user));
        when(sRepo.findById(2L)).thenReturn(Optional.of(school));
        when(rRepo.findByUserAndSchoolId(user, 2L)).thenReturn(List.of());

        ResponseEntity<?> resp = controller.createReminder(1L, 2L);
        assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    @DisplayName("createReminder successfully creates and saves a reminder")
    void createReminder_success() {
        ReminderRepository rRepo = mock(ReminderRepository.class);
        UserRepository uRepo = mock(UserRepository.class);
        SchoolRepository sRepo = mock(SchoolRepository.class);

        ReminderController controller = makeController(rRepo, uRepo, sRepo);

        User user = new User();
        School school = new School();
        school.setName("Test School");
        school.setProgramName("CS");
        school.setApplicationDeadline(LocalDate.now().toString());

        when(uRepo.findById(1L)).thenReturn(Optional.of(user));
        when(sRepo.findById(2L)).thenReturn(Optional.of(school));
        when(rRepo.findByUserAndSchoolId(user, 2L)).thenReturn(List.of());

        when(rRepo.save(any(Reminder.class))).thenAnswer(inv -> {
            Reminder rem = inv.getArgument(0);
            rem.setId(100L);
            return rem;
        });

        ResponseEntity<?> resp = controller.createReminder(1L, 2L);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        Reminder saved = (Reminder) resp.getBody();
        assertThat(saved.getId()).isEqualTo(100L);
        assertThat(saved.getIsCompleted()).isFalse();
    }

    @Test
    @DisplayName("getUserReminders returns 404 when user not found, or list when present")
    void getUserReminders_variants() {
        ReminderRepository rRepo = mock(ReminderRepository.class);
        UserRepository uRepo = mock(UserRepository.class);
        SchoolRepository sRepo = mock(SchoolRepository.class);
        ReminderController controller = makeController(rRepo, uRepo, sRepo);

        when(uRepo.findById(1L)).thenReturn(Optional.empty());
        ResponseEntity<List<Reminder>> r1 = controller.getUserReminders(1L);
        assertThat(r1.getStatusCode().value()).isEqualTo(404);

        User user = new User();
        when(uRepo.findById(2L)).thenReturn(Optional.of(user));
        when(rRepo.findByUser(user)).thenReturn(List.of(new Reminder()));

        ResponseEntity<List<Reminder>> r2 = controller.getUserReminders(2L);
        assertThat(r2.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(r2.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("deleteReminder handles user missing, reminder missing, and success")
    void deleteReminder_variants() {
        ReminderRepository rRepo = mock(ReminderRepository.class);
        UserRepository uRepo = mock(UserRepository.class);
        SchoolRepository sRepo = mock(SchoolRepository.class);
        ReminderController controller = makeController(rRepo, uRepo, sRepo);

        when(uRepo.findById(1L)).thenReturn(Optional.empty());
        ResponseEntity<?> r1 = controller.deleteReminder(10L, 1L);
        assertThat(r1.getStatusCode().value()).isEqualTo(404);

        User user = new User();
        when(uRepo.findById(2L)).thenReturn(Optional.of(user));
        when(rRepo.findByIdAndUser(10L, user)).thenReturn(Optional.empty());
        ResponseEntity<?> r2 = controller.deleteReminder(10L, 2L);
        assertThat(r2.getStatusCode().value()).isEqualTo(404);

        when(uRepo.findById(3L)).thenReturn(Optional.of(user));
        Reminder rem = new Reminder();
        when(rRepo.findByIdAndUser(11L, user)).thenReturn(Optional.of(rem));
        ResponseEntity<?> r3 = controller.deleteReminder(11L, 3L);
        assertThat(r3.getStatusCode().is2xxSuccessful()).isTrue();
        verify(rRepo).delete(rem);
    }

    @Test
    @DisplayName("deleteReminderBySchool handles missing user / no reminders / success")
    void deleteReminderBySchool_variants() {
        ReminderRepository rRepo = mock(ReminderRepository.class);
        UserRepository uRepo = mock(UserRepository.class);
        SchoolRepository sRepo = mock(SchoolRepository.class);
        ReminderController controller = makeController(rRepo, uRepo, sRepo);

        when(uRepo.findById(1L)).thenReturn(Optional.empty());
        ResponseEntity<?> r1 = controller.deleteReminderBySchool(5L, 1L);
        assertThat(r1.getStatusCode().value()).isEqualTo(404);

        User user = new User();
        when(uRepo.findById(2L)).thenReturn(Optional.of(user));
        when(rRepo.findByUserAndSchoolId(user, 5L)).thenReturn(List.of());
        ResponseEntity<?> r2 = controller.deleteReminderBySchool(5L, 2L);
        assertThat(r2.getStatusCode().value()).isEqualTo(404);

        when(rRepo.findByUserAndSchoolId(user, 5L)).thenReturn(List.of(new Reminder(), new Reminder()));
        ResponseEntity<?> r3 = controller.deleteReminderBySchool(5L, 2L);
        assertThat(r3.getStatusCode().is2xxSuccessful()).isTrue();
        verify(rRepo).deleteAll(anyList());
    }

    @Test
    @DisplayName("toggleReminderComplete flips completion flag")
    void toggleReminderComplete() {
        ReminderRepository rRepo = mock(ReminderRepository.class);
        UserRepository uRepo = mock(UserRepository.class);
        SchoolRepository sRepo = mock(SchoolRepository.class);
        ReminderController controller = makeController(rRepo, uRepo, sRepo);

        User user = new User();
        when(uRepo.findById(1L)).thenReturn(Optional.of(user));

        Reminder rem = new Reminder();
        rem.setIsCompleted(false);
        when(rRepo.findByIdAndUser(10L, user)).thenReturn(Optional.of(rem));
        when(rRepo.save(any(Reminder.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> r = controller.toggleReminderComplete(10L, 1L);
        assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
        Reminder updated = (Reminder) r.getBody();
        assertThat(updated.getIsCompleted()).isTrue();
    }
}
