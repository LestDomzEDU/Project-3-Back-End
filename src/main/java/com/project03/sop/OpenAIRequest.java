package com.project03.sop;

import java.util.List;

public class OpenAIRequest {
    private String model;
    private List<Message> messages;

    public OpenAIRequest() {
    }

    public OpenAIRequest(String model, String prompt) {
        this.model = model;
        this.messages = List.of(
                new Message("system", "You are a helpful assistant."),
                new Message("user", prompt));
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}
