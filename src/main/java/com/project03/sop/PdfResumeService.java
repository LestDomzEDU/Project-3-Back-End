package com.project03.sop;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
public class PdfResumeService {

    public String extractPdfText(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename() != null
                ? file.getOriginalFilename().toLowerCase()
                : "";

        if (!filename.endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF resumes are allowed.");
        }

        try (InputStream is = file.getInputStream();
                PDDocument document = PDDocument.load(is)) {

            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}
