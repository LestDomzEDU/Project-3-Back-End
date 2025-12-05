package com.project03.sop;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/sop")
public class SopController {

    private final PdfResumeService pdfResumeService;
    private final SopService sopService;

    public SopController(PdfResumeService pdfResumeService, SopService sopService) {
        this.pdfResumeService = pdfResumeService;
        this.sopService = sopService;
    }

    @PostMapping("/generate")
    public ResponseEntity<SopResponse> generateSop(
            @RequestParam("resume") MultipartFile resumeFile,
            @RequestParam("targetProgram") String targetProgram,
            @RequestParam("targetUniversity") String targetUniversity,
            @RequestParam(value = "extraNotes", required = false, defaultValue = "") String extraNotes) {
        try {
            String resumeText = pdfResumeService.extractPdfText(resumeFile);

            if (resumeText == null || resumeText.trim().length() < 50) {
                return ResponseEntity.badRequest()
                        .body(new SopResponse(
                                "",
                                "Resume text could not be extracted. Make sure your PDF has selectable text (not just a scan)."));
            }

            String sopDraft = sopService.generateSop(resumeText, targetProgram, targetUniversity, extraNotes);
            return ResponseEntity.ok(new SopResponse(sopDraft, null));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new SopResponse("", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(new SopResponse("", "Unexpected server error while generating SOP."));
        }
    }
}
