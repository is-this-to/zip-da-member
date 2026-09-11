package com.zipdamember.domain.file.service;

public record ValidatedDocument(
        byte[] content,
        String contentType,
        String extension
) {
}
