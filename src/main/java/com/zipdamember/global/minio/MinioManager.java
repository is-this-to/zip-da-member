package com.zipdamember.global.minio;

import com.zipdamember.global.config.minio.MinioConfig;
import com.zipdamember.global.error.custom.business.FileManagedException;
import io.minio.MinioClient;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioManager {

    private final MinioConfig minioConfig;
    private final MinioClient minioClient;

    /**
     * 해당 파일 저장 주소 만들기
     * @param file 저장할 파일
     * @return MinIO 버킷 내부에서 실제 파일 저장 경로
     */
    public String generateProfileObjectKey(MultipartFile file) {
        String extension = validateAndExtractExtension(file);
        return generateProfileObjectKey(extension);
    }

    public String generateProfileObjectKey(String extension) {
        // 예시 결과: profiles/20260909_550e8400-e29b-41d4-a716-446655440000.png
        String fileName = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "_" + UUID.randomUUID() + "." + extension;

        Path path = Path.of(minioConfig.minioProfilePath(), fileName).normalize();
        // MinIO는 /를 구분자로 사용, 윈도우 환경에서 서버를 돌리면 \가 들어가 에러 가능성 -> /로 바꿔주는 안전장치
        String objectKey = path.toString().replace(File.separatorChar, '/');

        if (objectKey.startsWith("../") || objectKey.length() > 255) {
            throw new FileManagedException("파일 저장 경로가 올바르지 않습니다.");
        }
        return objectKey;
    }

    public String generateDocumentObjectKey(String extension) {
        String fileName = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "_" + UUID.randomUUID() + "." + extension;
        Path path = Path.of(minioConfig.minioDocumentPath(), fileName).normalize();
        String objectKey = path.toString().replace(File.separatorChar, '/');

        if (objectKey.startsWith("../") || objectKey.length() > 255) {
            throw new FileManagedException("문서 저장 경로가 올바르지 않습니다.");
        }
        return objectKey;
    }

    /**
     * 파일 내용을 읽어서 이 파일만의 고유한 문자열을 만듦
     * @param file
     * @return 파일 전체를 읽어 만든 SHA-256 해시
     */
    public String calculateChecksum(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception exception) {
            throw new FileManagedException("파일 체크섬 생성에 실패했습니다.", exception);
        }
    }

    public String calculateChecksum(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception exception) {
            throw new FileManagedException("파일 체크섬 생성에 실패했습니다.", exception);
        }
    }

    /**
     * 실제 파일 바이트가 MinIO로 전송
     * @param objectKey 버킷 안에 저장될 파일 경로
     * @param file 저장될 파일
     */
    public void uploadFile(String objectKey, MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.minioBucket())
                            .object(objectKey)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        } catch (Exception exception) {
            throw new FileManagedException("MinIO 파일 업로드에 실패했습니다.", exception);
        }
    }

    public void uploadFile(String objectKey, byte[] content, String contentType) {
        try (InputStream inputStream = new ByteArrayInputStream(content)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.minioBucket())
                            .object(objectKey)
                            .stream(inputStream, content.length, -1)
                            .contentType(contentType)
                            .build()
            );
        } catch (Exception exception) {
            throw new FileManagedException("MinIO 파일 업로드에 실패했습니다.", exception);
        }
    }

    /**
     * 클라이언트가 접근 가능한 전체 객체 URL
     * @param objectKey 파일 저장 경로
     * @return 파일 최종 주소
     */
    public String createObjectUri(String objectKey) {
        String endpoint = minioConfig.minioEndpoint().replaceAll("/+$", "");
        Path objectPath = Path.of(minioConfig.minioBucket(), objectKey);
        return endpoint + "/" + objectPath.toString().replace(File.separatorChar, '/');
    }

    public String createPresignedGetUrl(String objectKey, int expirySeconds) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioConfig.minioBucket())
                            .object(objectKey)
                            .expiry(expirySeconds)
                            .build()
            );
        } catch (Exception exception) {
            throw new FileManagedException("비공개 파일 열람 주소 생성에 실패했습니다.", exception);
        }
    }

    public void deleteFileQuietly(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.minioBucket())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception exception) {
            log.error("DB 저장 실패 후 MinIO 파일 정리에 실패했습니다: objectKey={}", objectKey, exception);
        }
    }

    /**
     * 파일 유효설 검사
     * @param file
     * @return 확장자
     */
    private String validateAndExtractExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileManagedException("업로드할 파일이 없습니다.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new FileManagedException("파일 확장자를 확인할 수 없습니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null
                || minioConfig.allowImageExtensions() == null
                || !minioConfig.allowImageExtensions().contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new FileManagedException("허용하지 않는 이미지 형식입니다.");
        }

        String extension = originalFilename
                .substring(originalFilename.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);
        if (!isExtensionCompatible(contentType, extension)) {
            throw new FileManagedException("파일 확장자와 Content-Type이 일치하지 않습니다.");
        }
        return extension;
    }

    /**
     * HTTP 헤더의 ContentType과 실제 파일 확장자 같은지 비교
     * @param contentType HTTP 통신에서 전송하는 데이터 형식을 알려주는 헤더
     * @param extension 사용자가 보낸 파일 확장자
     * @return
     */
    private boolean isExtensionCompatible(String contentType, String extension) {
        String lowerExtension = extension.toLowerCase(Locale.ROOT);
        String lowerContentType = contentType.toLowerCase(Locale.ROOT);
        String expectedContentType = "image/" + lowerExtension;

        if(lowerExtension.equals("jpg") && lowerContentType.equals("image/jpeg") || lowerExtension.equals("jpeg") && lowerContentType.equals("image/jpg")) {
            return true;
        }

        return expectedContentType.equals(lowerContentType);
    }
}
