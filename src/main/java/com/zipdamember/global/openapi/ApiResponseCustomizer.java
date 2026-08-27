package com.zipdamember.global.openapi;


import com.zipdamember.global.response.constant.CustomResponseCode;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.View;

import java.util.*;

// OperationCustomizer: springdoc-openapi가 Swagger 문서를 만들때 각 API operation을 후처리할 수 있게 해주는 인터페이스
@Component
public class ApiResponseCustomizer implements OperationCustomizer {
    // OperationCustomizer: springdoc가 컨트롤러의 API 문서를 만든 후, 그 결과를 추가로 수정할 수 있게 해주는 인터페이스
    // operation: 현재 API의 Swagger 문서 정보
    // handlerMethod: 현재 API를 처리하는 컨트롤러 메서드 정보
    // 즉 handlerMethod를 통해 어노테이션을 읽고 operation에 Swagger 응답을 추가

    private final View error;

    public ApiResponseCustomizer(View error) {
        this.error = error;
    }

    // 어노테이션을 빌드될때 OperationCustomizer 구현체의 customize를 실행을함
    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {

        // 컨트롤러 메서드에 붙어 있는 @CustomApiResponse 정보 가져오기
        CustomApiResponse annotation = handlerMethod.getMethodAnnotation(CustomApiResponse.class);
        // 해당 어노테이션이 붙어 있지 않은 컨트롤러인 경우 원래 문서를 그대로 반환
        if (annotation == null) {
            return operation;
        }

        // Swagger을 쓸때 500 status라도 message가 다른 응답이 있기 때문에 List로 받음
        Map<Integer, List<CustomResponseCode>> errorCodeMap = new HashMap<>();

        // annotation에 저장된 value List를 Map에 저장함
        for(CustomResponseCode injectErrorCode : annotation.value()) {
            int httpStatus = injectErrorCode.getHttpStatus().value();
            // 기존에 있는 map에서 검색해서 해당 status가 없으면 List를 추가해서 map에 추가함
            // 기존에 있는 map에서 검색해서 해당 status가 있으면 기존에 있는 List에 CustomResponseCode add함
            /*
             * if(errorCodeMap.get(httpStatus) != null) {
             *      // 만약 해당 key의 ArrayList가 Map에 있을 경우
             *      errorCodeMap.get(httpStatus).add(injectErrorCode);
             *} else {
             *      // 해당 key의 ArrayList가 Map에 없을 경우
             *      errorCodeMap.put(httpStatus, new ArrayList<>(List.of(injectErrorCode)));
             * }
             */
            errorCodeMap.computeIfAbsent(httpStatus, item -> new ArrayList<>()).add(injectErrorCode);
        }

        errorCodeMap.forEach((httpStatus, customErrorCodeList) -> {
            // Swagger의 Content 어노테이션
            Content content = new Content();
            // Swagger의 MediaType 어노테이션
            MediaType mediaType = new MediaType();

            customErrorCodeList.forEach(customErrorCode -> {
                Map<String, Object> exampleMap = new LinkedHashMap<>();
                exampleMap.put("code", customErrorCode.getCode());
                exampleMap.put("message", customErrorCode.name());
                exampleMap.put("data", null);
                mediaType.addExamples(customErrorCode.name(), new Example().value(exampleMap));
            });
            content.addMediaType("application/json", mediaType);

            operation.getResponses().addApiResponse(
                    String.valueOf(httpStatus),
                    new ApiResponse().description("에러 응답").content(content)
            );
        });

        return operation;
    }
}