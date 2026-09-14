package com.zipdamember.global.config.minio;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "minio")
public record MinioConfig(
        String minioEndpoint,
        String minioBucket,
        String minioAccessKey,
        String minioSecretKey,
        String minioProfilePath,
        String minioDocumentPath,
        List<String> allowImageExtensions
) {
}
