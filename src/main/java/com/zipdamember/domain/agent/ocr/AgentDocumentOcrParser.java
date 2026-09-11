package com.zipdamember.domain.agent.ocr;

import com.zipdamember.domain.agent.constant.AgentApplicationDocumentType;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AgentDocumentOcrParser {

    private static final Pattern BUSINESS_NUMBER = Pattern.compile(
            "(?<!\\d)(\\d{3})\\s*[-–—]?\\s*(\\d{2})\\s*[-–—]?\\s*(\\d{5})(?!\\d)"
    );
    private static final Pattern START_DATE = Pattern.compile(
            "개업\\s*연월일\\s*[:：]?\\s*(\\d{4})\\s*[년./-]\\s*(\\d{1,2})\\s*[월./-]\\s*(\\d{1,2})\\s*일?"
    );
    private static final Pattern COMPACT_START_DATE = Pattern.compile(
            "개업\\s*연월일\\s*[:：]?\\s*(\\d{4})(\\d{2})(\\d{2})"
    );
    private static final Pattern REPRESENTATIVE_NAME = Pattern.compile(
            "(?:성명\\s*\\(\\s*대표자\\s*\\)|대표자(?:명)?)\\s*[:：]?\\s*([가-힣A-Za-z]{2,30})"
    );
    private static final Pattern MODERN_AGENT_NUMBER = Pattern.compile(
            "(?:제\\s*)?([0-9]{5,8}\\s*[-‐‑‒–—]\\s*[0-9]{4}\\s*[-‐‑‒–—]\\s*[0-9]{3,6})(?:\\s*호)?"
    );
    private static final Pattern LEGACY_AGENT_NUMBER = Pattern.compile(
            "(?:제\\s*)?([가-힣A-Za-z]\\s*[-‐‑‒–—]\\s*[0-9]{3,8}\\s*[-‐‑‒–—]\\s*[0-9]{1,6})(?:\\s*호)?"
    );
    private static final Pattern LABELED_AGENT_NUMBER = Pattern.compile(
            "(?m)(?:개설\\s*)?등록번호\\s*[:：]?\\s*(?:제\\s*)?([가-힣A-Za-z0-9]+(?:\\s*[-‐‑‒–—]\\s*[가-힣A-Za-z0-9]+){0,3})(?:\\s*호)?"
    );
    private static final Pattern AGENCY_NAME = Pattern.compile(
            "(?m)(?:중개사무소\\s*명칭|상호(?:명)?)\\s*[:：]?\\s*([^\\r\\n]{2,60})"
    );

    public DocumentOcrResult parse(
            AgentApplicationDocumentType documentType,
            String rawText
    ) {
        String normalizedText = normalizeText(rawText);
        String representativeName = extractGroup(REPRESENTATIVE_NAME, normalizedText);

        if (documentType == AgentApplicationDocumentType.BUSINESS_LICENSE) {
            return new DocumentOcrResult(
                    rawText,
                    extractBusinessNumber(normalizedText),
                    extractStartDate(normalizedText),
                    representativeName,
                    null,
                    null
            );
        }

        return new DocumentOcrResult(
                rawText,
                null,
                null,
                representativeName,
                extractAgentRegistrationNumber(normalizedText),
                extractAgencyName(normalizedText)
        );
    }

    private String extractBusinessNumber(String text) {
        Matcher matcher = BUSINESS_NUMBER.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1) + matcher.group(2) + matcher.group(3);
    }

    private LocalDate extractStartDate(String text) {
        Matcher matcher = START_DATE.matcher(text);
        if (!matcher.find()) {
            matcher = COMPACT_START_DATE.matcher(text);
            if (!matcher.find()) {
                return null;
            }
        }

        try {
            return LocalDate.of(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3))
            );
        } catch (DateTimeException exception) {
            return null;
        }
    }

    private String extractAgentRegistrationNumber(String text) {
        String candidate = extractGroup(MODERN_AGENT_NUMBER, text);
        if (candidate == null) {
            candidate = extractGroup(LEGACY_AGENT_NUMBER, text);
        }
        if (candidate == null) {
            candidate = extractGroup(LABELED_AGENT_NUMBER, text);
        }
        return normalizeAgentRegistrationNumber(candidate);
    }

    private String extractAgencyName(String text) {
        String value = extractGroup(AGENCY_NAME, text);
        if (value == null) {
            return null;
        }
        value = value.replaceFirst(
                "\\s+(?:중개사무소\\s*소재지|소재지|성명|생년월일).*$",
                ""
        ).trim();
        return value.length() > 50 ? value.substring(0, 50) : value;
    }

    private String normalizeAgentRegistrationNumber(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value
                .replaceAll("[‐‑‒–—]", "-")
                .replaceAll("\\s*-\\s*", "-")
                .replaceAll("\\s+", "")
                .replaceFirst("^제", "")
                .replaceFirst("호$", "");
        if (normalized.length() < 3 || normalized.length() > 20
                || !normalized.matches(".*\\d.*")) {
            return null;
        }
        return normalized;
    }

    private String extractGroup(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace('\u00A0', ' ')
                .replace("\r\n", "\n")
                .replace('\r', '\n');
    }
}
