package com.zipdamember.global.config.minio;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioClientConfig {

    /**
     * MinIO SDK가 실제 스토리지 서버와 통신할 MinioClient를 Spring Bean으로 등록
     * @param minioConfig MinIO 환경 변수 객체
     * @return
     */
    @Bean
    public MinioClient minioClient(MinioConfig minioConfig) {
        return MinioClient.builder()
                .endpoint(minioConfig.minioEndpoint())
                .credentials(
                        minioConfig.minioAccessKey(),
                        minioConfig.minioSecretKey()
                )
                .build();
    }
}
