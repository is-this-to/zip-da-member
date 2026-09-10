package com.zipdamember.domain.agent.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.zipdamember.domain.agent.entity.AgentApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class NtsBusinessValidationClient {
    private static final String BASE_URL = "https://api.odcloud.kr";
    private static final String VALIDATE_PATH = "/api/nts-businessman/v1/validate";

    private final RestClient restClient;
    private final String apiKey;

    public NtsBusinessValidationClient(
            RestClient.Builder restClientBuilder,
            @Value("${NTS_API_KEY:}") String apiKey
    ) {
        this.restClient = restClientBuilder.baseUrl(BASE_URL).build();
        this.apiKey = apiKey;
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public NtsBusinessValidationResult validate(AgentApplication application) {
        if (!isConfigured()) {
            return NtsBusinessValidationResult.error();
        }

        try {
            JsonNode response = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(VALIDATE_PATH)
                            .queryParam("serviceKey", apiKey)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new NtsBusinessValidationRequest(List.of(
                            new Business(
                                    normalizeBusinessNumber(application.getRequestBusinessNo()),
                                    application.getRequestStartDate().format(DateTimeFormatter.BASIC_ISO_DATE),
                                    application.getRequestRepresentativeName()
                            )
                    )))
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && "OK".equals(response.path("status_code").asText())) {
                return NtsBusinessValidationResult.matched();
            }
            return NtsBusinessValidationResult.error();
        } catch (RuntimeException exception) {
            return NtsBusinessValidationResult.error();
        }
    }

    private String normalizeBusinessNumber(String businessNumber) {
        return businessNumber == null ? null : businessNumber.replace("-", "");
    }

    private record NtsBusinessValidationRequest(List<Business> businesses) {
    }

    private record Business(
            String b_no,
            String start_dt,
            String p_nm
    ) {
    }
}
