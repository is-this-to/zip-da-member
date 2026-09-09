package com.zipdamember.domain.agent.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.zipdamember.domain.agent.entity.AgentApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

@Component
public class MolitAgencyRegistrationClient {
    private static final String BASE_URL = "https://api.vworld.kr";
    private static final String OFFICE_INFO_PATH = "/ned/data/getEBOfficeInfo";

    private final RestClient restClient;
    private final String apiKey;

    public MolitAgencyRegistrationClient(
            RestClient.Builder restClientBuilder,
            @Value("${MOLIT_API_KEY:}") String apiKey
    ) {
        this.restClient = restClientBuilder.baseUrl(BASE_URL).build();
        this.apiKey = apiKey;
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public MolitAgencyRegistrationResult validate(AgentApplication application) {
        if (!isConfigured()) {
            return MolitAgencyRegistrationResult.error();
        }

        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> buildRequestUri(uriBuilder, application))
                    .retrieve()
                    .body(JsonNode.class);

            return parseResponse(response, application);
        } catch (RuntimeException exception) {
            return MolitAgencyRegistrationResult.error();
        }
    }

    private java.net.URI buildRequestUri(UriBuilder uriBuilder, AgentApplication application) {
        uriBuilder.path(OFFICE_INFO_PATH)
                .queryParam("key", apiKey)
                .queryParam("jurirno", application.getRequestAgencyRegistrationNo())
                .queryParam("format", "json")
                .queryParam("numOfRows", 1)
                .queryParam("pageNo", 1);

        if (hasText(application.getRequestAgencyName())) {
            uriBuilder.queryParam("bsnmCmpnm", application.getRequestAgencyName());
        }
        if (hasText(application.getRequestRepresentativeName())) {
            uriBuilder.queryParam("brkrNm", application.getRequestRepresentativeName());
        }
        return uriBuilder.build();
    }

    private MolitAgencyRegistrationResult parseResponse(
            JsonNode response,
            AgentApplication application
    ) {
        if (response == null) {
            return MolitAgencyRegistrationResult.error();
        }

        JsonNode offices = response.path("EBOffices");
        if (offices.isMissingNode()) {
            JsonNode noResultResponse = response.path("response");
            if ("0".equals(noResultResponse.path("totalCount").asText())) {
                return MolitAgencyRegistrationResult.mismatched();
            }
            return MolitAgencyRegistrationResult.error();
        }

        if (offices.path("totalCount").asInt() < 1) {
            return MolitAgencyRegistrationResult.mismatched();
        }

        JsonNode fields = offices.path("field");
        if (!fields.isArray() || fields.isEmpty()) {
            return MolitAgencyRegistrationResult.error();
        }

        JsonNode office = fields.get(0);
        if (!isSameOffice(office, application)) {
            return MolitAgencyRegistrationResult.mismatched();
        }

        return MolitAgencyRegistrationResult.matched(
                office.path("sttusSeCodeNm").asText(null),
                office.path("rnmadr").asText(null),
                null
        );
    }

    private boolean isSameOffice(JsonNode office, AgentApplication application) {
        return normalizeRegistrationNumber(office.path("jurirno").asText())
                .equals(normalizeRegistrationNumber(application.getRequestAgencyRegistrationNo()))
                && normalizeText(office.path("bsnmCmpnm").asText())
                .equals(normalizeText(application.getRequestAgencyName()))
                && normalizeText(office.path("brkrNm").asText())
                .equals(normalizeText(application.getRequestRepresentativeName()));
    }

    private String normalizeRegistrationNumber(String registrationNumber) {
        return registrationNumber == null ? "" : registrationNumber.replaceAll("[\\s-]", "");
    }

    private String normalizeText(String text) {
        return text == null ? "" : text.replaceAll("\\s", "");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
