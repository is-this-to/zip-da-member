package com.zipdamember.global.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI().info(
                new Info()
                        .title("ZIPDA member API") // 문서 이름
                        .description("ZIPDA member REST API Document") // 문서 설명
                        .version("v1.0.0")
        )
        // 인증이 필요한 경우에 토큰을 가진 상태에서 요청을 보내볼수있도록 설정하는것
        .components(new Components().addSecuritySchemes(BEARER_AUTH,
                new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"))
//                .addSecuritySchemes(
//                        COOKIE_REFRESH_TOKEN,
//                        new SecurityScheme()
//                                .type(SecurityScheme.Type.)
//                )
        )
        .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
