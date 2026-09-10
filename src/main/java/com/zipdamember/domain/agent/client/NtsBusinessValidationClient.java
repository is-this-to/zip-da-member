package com.zipdamember.domain.agent.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.zipdamember.domain.agent.entity.AgentApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class NtsBusinessValidationClient {
    private final RestClient restClient;
    private final String businessValidationUrl;
    private final String apiKey;

    public NtsBusinessValidationClient(
            RestClient.Builder restClientBuilder,
            @Value("${external.nts.base-url:}") String businessValidationUrl,
            @Value("${external.nts.api-key:}") String apiKey
    ) {
        this.restClient = restClientBuilder.build();
        this.businessValidationUrl = businessValidationUrl;
        this.apiKey = apiKey;
    }

    public boolean isConfigured() {
        return !businessValidationUrl.isBlank() && !apiKey.isBlank();
    }

    public NtsBusinessValidationResult validate(AgentApplication application) {
        if (!isConfigured()) {
            return NtsBusinessValidationResult.error();
        }

        try {
            JsonNode response = restClient.post()
                    .uri(buildRequestUri())
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

    private URI buildRequestUri() {
        return UriComponentsBuilder.fromUriString(businessValidationUrl)
                .queryParam("serviceKey", apiKey)
                .build()
                .encode()
                .toUri();
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
