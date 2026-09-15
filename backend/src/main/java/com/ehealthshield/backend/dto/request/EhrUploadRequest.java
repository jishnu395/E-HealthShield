package com.ehealthshield.backend.dto.request;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record EhrUploadRequest(
        MultipartFile file,
        String patientWallet,
        String uploaderWallet,
        List<String> keywords
) {
    public EhrUploadRequest {
        if (keywords == null) {
            keywords = List.of();
        }
    }
}
