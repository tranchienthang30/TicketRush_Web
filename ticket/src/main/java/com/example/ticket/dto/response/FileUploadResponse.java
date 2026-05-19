package com.example.ticket.dto.response;

public record FileUploadResponse(
        String url,
        String filename,
        long size
) {
}
