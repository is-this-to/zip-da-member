package com.zipdamember.domain.agent.ocr;

public interface DocumentTextExtractor {

    String extract(byte[] content, String contentType);
}
