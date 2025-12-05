package com.project03.sop;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class SopService {

    @Value("${openai.api.key}")
    private String openaiApiKey;

    private final WebClient webClient = WebClient.create("https://api.openai.com");

    public String generateSop(String resumeText, String targetProgram, String targetUniversity, String extraNotes) {

        String prompt = """
                You are an assistant that writes a first-draft Statement of Purpose for a master's application.
                Use ONLY the information from the resume text below.

                Target Program: %s
                Target University: %s
                Extra Notes: %s

                Write 800–1200 words, first-person, professional tone.

                Resume:
                %s
                """.formatted(
                targetProgram,
                targetUniversity,
                extraNotes == null ? "" : extraNotes,
                resumeText);

        OpenAIRequest request = new OpenAIRequest("gpt-4o-mini", prompt);

        OpenAIResponse response = webClient.post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer " + openaiApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenAIResponse.class)
                .block();

        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new RuntimeException("OpenAI response was empty");
        }

        return response.getChoices().get(0).getMessage().getContent().trim();
    }
}
