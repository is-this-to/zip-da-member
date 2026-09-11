package com.zipdamember.domain.agent.client;

import com.zipdamember.domain.agent.entity.AgentApplication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import tools.jackson.databind.JsonNode;

@Component
@Slf4j
public class MolitAgencyRegistrationClient {
    private final RestClient restClient;
    private final String officeInfoUrl;
    private final String apiKey;
    private final String domain;

    public MolitAgencyRegistrationClient(
            RestClient.Builder restClientBuilder,
            @Value("${external.molit.office-info-url:}") String officeInfoUrl,
            @Value("${external.molit.api-key:}") String apiKey,
            @Value("${external.molit.domain:}") String domain
    ) {
        this.restClient = restClientBuilder.build();
        this.officeInfoUrl = officeInfoUrl;
        this.apiKey = apiKey;
        this.domain = domain;
    }

    public boolean isConfigured() {
        return hasText(officeInfoUrl) && hasText(apiKey) && hasText(domain);
    }

    public MolitAgencyRegistrationResult validate(AgentApplication application) {
        if (!isConfigured()) {
            return MolitAgencyRegistrationResult.error();
        }

        try {
            JsonNode response = restClient.get()
                    .uri(buildRequestUri(application))
                    .retrieve()
                    .body(JsonNode.class);

            return parseResponse(response, application);
        } catch (RuntimeException exception) {
            log.warn("국토교통부 중개사업소정보조회 API 호출 실패: exceptionType={}, causeType={}",
                    exception.getClass().getSimpleName(),
                    resolveCauseType(exception));
            return MolitAgencyRegistrationResult.error();
        }
    }

    private URI buildRequestUri(AgentApplication application) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(officeInfoUrl)
                .queryParam("key", apiKey)
                .queryParam("domain", domain)
                .queryParam("jurirno", application.getRequestAgencyRegistrationNo())
                .queryParam("format", "json")
                .queryParam("numOfRows", 1)
                .queryParam("pageNo", 1);
        return uriBuilder.build().encode().toUri();
    }

    private MolitAgencyRegistrationResult parseResponse(
            JsonNode response,
            AgentApplication application
    ) {
        if (response == null) {
            return MolitAgencyRegistrationResult.error();
        }

        JsonNode offices = response.path("EDOffices");
        if (offices.isMissingNode()) {
            JsonNode noResultResponse = response.path("response");
            if ("0".equals(noResultResponse.path("totalCount").asText())) {
                return MolitAgencyRegistrationResult.mismatched();
            }
            log.warn("국토교통부 중개사업소정보조회 API 응답 형식 오류");
            return MolitAgencyRegistrationResult.error();
        }

        if (hasText(offices.path("resultCode").asText())) {
            log.warn("국토교통부 중개사업소정보조회 API 응답 오류: resultCode={}",
                    offices.path("resultCode").asText());
            return MolitAgencyRegistrationResult.error();
        }

        if (offices.path("totalCount").asInt() < 1) {
            return MolitAgencyRegistrationResult.mismatched();
        }

        JsonNode fields = offices.path("field");
        if (!fields.isArray() || fields.isEmpty()) {
            log.warn("국토교통부 중개사업소정보조회 API 응답 필드 오류: totalCount={}",
                    offices.path("totalCount").asText());
            return MolitAgencyRegistrationResult.error();
        }

        JsonNode office = fields.get(0);
        if (!isSameRegistrationNumber(office, application)) {
            return MolitAgencyRegistrationResult.mismatched();
        }

        return MolitAgencyRegistrationResult.matched(
                office.path("sttusSeCodeNm").asText(null),
                office.path("rdnmadr").asText(null),
                office.path("mnnmadr").asText(null)
        );
    }

    private boolean isSameRegistrationNumber(JsonNode office, AgentApplication application) {
        return normalizeRegistrationNumber(office.path("jurirno").asText())
                .equals(normalizeRegistrationNumber(application.getRequestAgencyRegistrationNo()));
    }

    private String normalizeRegistrationNumber(String registrationNumber) {
        return registrationNumber == null ? "" : registrationNumber.replaceAll("[\\s-]", "");
    }

    private String resolveCauseType(RuntimeException exception) {
        Throwable cause = exception.getCause();
        return cause == null ? "NONE" : cause.getClass().getSimpleName();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
