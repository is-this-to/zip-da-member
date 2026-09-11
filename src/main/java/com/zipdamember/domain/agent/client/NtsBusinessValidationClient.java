package com.zipdamember.domain.agent.client;

import com.zipdamember.domain.agent.entity.AgentApplication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import tools.jackson.databind.JsonNode;

@Component
@Slf4j
public class NtsBusinessValidationClient {
    private final RestClient restClient;
    private final String businessValidationUrl;
    private final String apiKey;

    public NtsBusinessValidationClient(
            RestClient.Builder restClientBuilder,
            @Value("${external.nts.business-validation-url:}") String businessValidationUrl,
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

            if (response == null || !"OK".equals(response.path("status_code").asText())) {
                log.warn("국세청 사업자등록정보 진위확인 API 응답 비정상: statusCode={}",
                        response == null ? "EMPTY" : response.path("status_code").asText());
                return NtsBusinessValidationResult.error();
            }

            JsonNode businesses = response.path("data");
            if (!businesses.isArray() || businesses.isEmpty()) {
                log.warn("국세청 사업자등록정보 진위확인 API 응답 데이터 오류");
                return NtsBusinessValidationResult.error();
            }

            JsonNode business = businesses.get(0);
            String valid = business.path("valid").asText();
            if ("01".equals(valid)) {
                JsonNode status = business.path("status");
                return NtsBusinessValidationResult.matched(
                        status.path("b_stt").asText(null),
                        status.path("tax_type").asText(null),
                        parseClosedAt(status.path("end_dt").asText(null))
                );
            }
            if ("02".equals(valid)) {
                return NtsBusinessValidationResult.mismatched();
            }

            log.warn("국세청 사업자등록정보 진위확인 API 유효성 판정 오류: valid={}", valid);
            return NtsBusinessValidationResult.error();
        } catch (RuntimeException exception) {
            log.warn("국세청 사업자등록정보 진위확인 API 호출 실패: exceptionType={}, causeType={}",
                    exception.getClass().getSimpleName(),
                    resolveCauseType(exception));
            return NtsBusinessValidationResult.error();
        }
    }

    private String normalizeBusinessNumber(String businessNumber) {
        return businessNumber == null ? null : businessNumber.replace("-", "");
    }

    private LocalDate parseClosedAt(String closedAt) {
        if (closedAt == null || closedAt.isBlank()) {
            return null;
        }
        return LocalDate.parse(closedAt, DateTimeFormatter.BASIC_ISO_DATE);
    }

    private String resolveCauseType(RuntimeException exception) {
        Throwable cause = exception.getCause();
        return cause == null ? "NONE" : cause.getClass().getSimpleName();
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
