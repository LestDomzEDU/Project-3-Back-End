package com.project03.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.project03.model.User;
import com.project03.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    private User makeUser(long id, String email, String name) {
        User u = new User();
        u.setEmail(email);
        u.setName(name);
        // id is generated, but we can simulate via reflection-free pattern:
        // User has a setId? If not, we just rely on repository returning a user
        // with getId() preset by the mock.
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(u, id);
        } catch (Exception ignored) {}
        return u;
    }

    @Test
    @DisplayName("POST /api/users with missing email returns 400")
    void createUserMissingEmail() throws Exception {
        mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Test User\"}"))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.error").value("Email is required"));
    }

    @Test
    @DisplayName("POST /api/users returns existing user when email already present")
    void createUserExisting() throws Exception {
        User existing = makeUser(1L, "test@example.com", "Existing User");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existing));

        mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"test@example.com\",\"name\":\"Test User\"}"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.message").value("User already exists"))
               .andExpect(jsonPath("$.userId").value(1))
               .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @DisplayName("POST /api/users creates a new user when email not found")
    void createUserNew() throws Exception {
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

        User saved = makeUser(42L, "new@example.com", "New User");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"new@example.com\",\"name\":\"New User\"}"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.message").value("User created successfully"))
               .andExpect(jsonPath("$.userId").value(42))
               .andExpect(jsonPath("$.email").value("new@example.com"))
               .andExpect(jsonPath("$.name").value("New User"));
    }

    @Test
    @DisplayName("GET /api/users/{id}/student-id returns 404 for missing user")
    void getStudentIdNotFound() throws Exception {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/99/student-id"))
               .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/users/{id}/student-id returns student info for existing user")
    void getStudentId() throws Exception {
        User u = makeUser(5L, "s@example.com", "Student");
        when(userRepository.findById(5L)).thenReturn(Optional.of(u));

        mockMvc.perform(get("/api/users/5/student-id"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.studentId").value("5"))
               .andExpect(jsonPath("$.userId").value(5))
               .andExpect(jsonPath("$.email").value("s@example.com"));
    }

    @Test
    @DisplayName("GET /api/users lists all users")
    void getAllUsers() throws Exception {
        User u1 = makeUser(1L, "u1@example.com", "U1");
        User u2 = makeUser(2L, "u2@example.com", "U2");
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        mockMvc.perform(get("/api/users"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", Matchers.hasSize(2)))
               .andExpect(jsonPath("$[0].userId").value(1))
               .andExpect(jsonPath("$[1].userId").value(2));
    }

    @Test
    @DisplayName("GET /api/users/{id} returns user or 404")
    void getUserById() throws Exception {
        when(userRepository.findById(10L)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/users/10"))
               .andExpect(status().isNotFound());

        User u = makeUser(7L, "u7@example.com", "User7");
        when(userRepository.findById(7L)).thenReturn(Optional.of(u));
        mockMvc.perform(get("/api/users/7"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.userId").value(7))
               .andExpect(jsonPath("$.email").value("u7@example.com"));
    }
}
