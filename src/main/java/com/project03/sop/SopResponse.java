package com.project03.sop;

public class SopResponse {
    private String sopDraft;
    private String error;

    public SopResponse() {
    }

    public SopResponse(String sopDraft, String error) {
        this.sopDraft = sopDraft;
        this.error = error;
    }

    public String getSopDraft() {
        return sopDraft;
    }

    public void setSopDraft(String sopDraft) {
        this.sopDraft = sopDraft;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
