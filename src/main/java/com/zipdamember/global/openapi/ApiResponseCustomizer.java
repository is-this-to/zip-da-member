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

@Component
public class ApiResponseCustomizer implements OperationCustomizer {
    private final View error;

    public ApiResponseCustomizer(View error) {
        this.error = error;
    }
    // OperationCustomizer: 어노테이션을 빌드될때 OperationCustomizer 구현체의 customize를 실행을함

    // Operation 객체가 어노테이션 실행하기 위해 필요한 최상위 객체
    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        // 우리가 만든 어노테이션 정보 가져오기 -> 이 어노테이션 안에 value같은 정보가 들어가가 있음
        CustomApiResponse annotation = handlerMethod.getMethodAnnotation(CustomApiResponse.class);
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