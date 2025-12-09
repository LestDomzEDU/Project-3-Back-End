package com.project03.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@WebMvcTest(OAuthFinalController.class)
@AutoConfigureMockMvc(addFilters = false)
class OAuthFinalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /oauth2/final returns HTML that points to /api/me")
    void finalPageContainsRedirectScript() throws Exception {
        mockMvc.perform(get("/oauth2/final"))
               .andExpect(status().isOk())
               .andExpect(content().string(Matchers.containsString("<html")))
               .andExpect(content().string(Matchers.containsString("/api/me")));
    }
}
