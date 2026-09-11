package com.zipdamember.domain.file.service;

import com.zipdamember.domain.file.constant.FileCategory;
import com.zipdamember.domain.file.entity.FileObject;
import com.zipdamember.domain.file.constant.FileVisibility;
import com.zipdamember.domain.file.repository.FileObjectRepository;
import com.zipdamember.domain.file.response.FileUploadResponse;
import com.zipdamember.global.error.custom.business.FileManagedException;
import com.zipdamember.global.minio.MinioManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private static final int TEMPORARY_PROFILE_EXPIRY_HOURS = 24;
    private static final int MAX_SOCIAL_PROFILE_SIZE_BYTES = 10 * 1024 * 1024;
    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/jpg", "jpg",
            "image/png", "png",
            "image/gif", "gif",
            "image/webp", "webp"
    );

    private static final int MAX_AGENT_DOCUMENT_SIZE_BYTES = 10 * 1024 * 1024;
    private static final int PRIVATE_URL_EXPIRY_SECONDS = 300;
    private static final Map<String, String> AGENT_DOCUMENT_EXTENSIONS = Map.of(
            "application/pdf", "pdf",
            "image/jpeg", "jpg",
            "image/jpg", "jpg",
            "image/png", "png"
    );

    private final MinioManager minioManager;
    private final FileObjectRepository fileObjectRepository;

    /**
     * 프로필 파일 저장
     * @param file 저장할 프로필 파일
     * @return 저장한 파일 식별자와 주소
     */
    @Transactional
    public FileUploadResponse uploadProfile(MultipartFile file) {
        String objectKey = minioManager.generateProfileObjectKey(file);
        String fileUri = minioManager.createObjectUri(objectKey);
        String checksum = minioManager.calculateChecksum(file);

        minioManager.uploadFile(objectKey, file);

        try {
            FileObject fileObject = FileObject.createTemporaryProfile(
                    objectKey,
                    fileUri,
                    file.getContentType(),
                    file.getSize(),
                    checksum,
                    LocalDateTime.now().plusHours(TEMPORARY_PROFILE_EXPIRY_HOURS)
            );

            // saveAndFlush(): 바로 INSERT SQL을 즉시 DB에 전송
            FileObject savedFile = fileObjectRepository.saveAndFlush(fileObject);
            log.debug("프로필 파일 임시 저장 완료: fileId={}", savedFile.getFileId());
            return FileUploadResponse.from(savedFile);
        } catch (RuntimeException exception) {
            minioManager.deleteFileQuietly(objectKey);
            throw exception;
        }
    }

    /**
     * 사용자 회원가입 후 프로필 사진 외래키 연결
     * @param profileFileId 파일 식별자
     * @param memberId 연결할 회원 식별자 외래키
     * @param now 현재 시간ㄴ
     */
    @Transactional
    public void assignProfileToMember(Long profileFileId, Long memberId, LocalDateTime now) {
        log.debug("프로필 파일 회원 연결 조회: profileFileId={}, memberId={}", profileFileId, memberId);
        FileObject profileFile = fileObjectRepository.findByIdForUpdate(profileFileId)
                .orElseThrow(() -> {
                    log.warn("프로필 파일 조회 실패: profileFileId={}", profileFileId);
                    return new FileManagedException("프로필 파일 정보를 찾을 수 없습니다.");
                });

        if (profileFile.getCategory() != FileCategory.PROFILE) {
            throw new FileManagedException("프로필 용도로 업로드한 파일이 아닙니다.");
        }

        if (profileFile.isOwned()) {
            throw new FileManagedException("이미 다른 회원이 사용한 프로필 파일입니다.");
        }

        if (profileFile.isExpired(now)) {
            throw new FileManagedException("프로필 파일의 임시 보관 기간이 만료되었습니다.");
        }

        profileFile.assignOwner(memberId);
    }

    /**
     * 카카오 CDN의 프로필 이미지를 ZIPDA MinIO로 복사하고 회원 소유 파일로 등록한다.
     * 선택 동의 정보이므로 다운로드 실패 시 회원가입은 계속하고 프로필은 null로 둔다.
     */
    @Transactional
    public Long importKakaoProfile(String profileImageUrl, Long memberId) {
        if (profileImageUrl == null || profileImageUrl.isBlank()) {
            return null;
        }

        String objectKey = null;
        try {
            URI uri = URI.create(profileImageUrl);
            validateKakaoProfileUri(uri);

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<InputStream> response = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                throw new FileManagedException("카카오 프로필 이미지를 내려받을 수 없습니다.");
            }

            String contentType = response.headers().firstValue("Content-Type")
                    .map(value -> value.split(";", 2)[0].trim().toLowerCase(Locale.ROOT))
                    .orElseThrow(() -> new FileManagedException("카카오 프로필 이미지 형식을 확인할 수 없습니다."));
            String extension = IMAGE_EXTENSIONS.get(contentType);
            if (extension == null) {
                throw new FileManagedException("허용하지 않는 카카오 프로필 이미지 형식입니다.");
            }

            byte[] content;
            try (InputStream inputStream = response.body()) {
                content = readLimited(inputStream);
            }

            objectKey = minioManager.generateProfileObjectKey(extension);
            minioManager.uploadFile(objectKey, content, contentType);
            FileObject fileObject = FileObject.createOwnedProfile(
                    memberId,
                    objectKey,
                    minioManager.createObjectUri(objectKey),
                    contentType,
                    content.length,
                    minioManager.calculateChecksum(content)
            );
            return fileObjectRepository.saveAndFlush(fileObject).getFileId();
        } catch (Exception exception) {
            if (objectKey != null) {
                minioManager.deleteFileQuietly(objectKey);
            }
            log.warn("카카오 프로필 이미지 저장을 건너뜁니다: memberId={}", memberId, exception);
            return null;
        }
    }

    @Transactional
    public FileUploadResponse uploadOwnedProfile(MultipartFile file, Long memberId, FileCategory category) {
        String objectKey = minioManager.generateProfileObjectKey(file);
        String fileUri = minioManager.createObjectUri(objectKey);
        String checksum = minioManager.calculateChecksum(file);
        minioManager.uploadFile(objectKey, file);
        try {
            FileObject fileObject = FileObject.createOwnedPublicImage(
                    memberId, category, objectKey, fileUri,
                    file.getContentType(), file.getSize(), checksum
            );
            return FileUploadResponse.from(fileObjectRepository.saveAndFlush(fileObject));
        } catch (RuntimeException exception) {
            minioManager.deleteFileQuietly(objectKey);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public void validateOwnedProfile(Long fileId, Long memberId, FileCategory category) {
        FileObject file = fileObjectRepository.findById(fileId)
                .orElseThrow(() -> new FileManagedException("프로필 파일을 찾을 수 없습니다."));
        if (!memberId.equals(file.getOwnerMemberId()) || file.getCategory() != category) {
            throw new FileManagedException("본인이 업로드한 올바른 프로필 파일이 아닙니다.");
        }
    }

    @Transactional(readOnly = true)
    public String getPublicFileUri(Long fileId) {
        if (fileId == null) return null;
        return fileObjectRepository.findById(fileId)
                .map(FileObject::getFileUri)
                .orElse(null);
    }

    public ValidatedDocument prepareAgentDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileManagedException("업로드할 신청 서류가 없습니다.");
        }
        if (file.getSize() > MAX_AGENT_DOCUMENT_SIZE_BYTES) {
            throw new FileManagedException("신청 서류는 10MB 이하만 업로드할 수 있습니다.");
        }

        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        String extension = AGENT_DOCUMENT_EXTENSIONS.get(contentType);
        if (extension == null) {
            throw new FileManagedException("신청 서류는 PDF, JPG, PNG 형식만 업로드할 수 있습니다.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new FileManagedException("신청 서류의 파일 확장자를 확인할 수 없습니다.");
        }
        String requestedExtension = originalFilename
                .substring(originalFilename.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);
        if (!(requestedExtension.equals(extension)
                || ("jpg".equals(extension) && "jpeg".equals(requestedExtension)))) {
            throw new FileManagedException("신청 서류의 확장자와 Content-Type이 일치하지 않습니다.");
        }

        try {
            byte[] content = file.getBytes();
            if (!hasExpectedSignature(contentType, content)) {
                throw new FileManagedException("신청 서류의 실제 파일 형식이 올바르지 않습니다.");
            }
            return new ValidatedDocument(content, contentType, extension);
        } catch (FileManagedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new FileManagedException("신청 서류를 읽을 수 없습니다.", exception);
        }
    }

    @Transactional
    public FileObject storePrivateAgentDocument(
            ValidatedDocument document,
            Long ownerMemberId,
            FileCategory category
    ) {
        String objectKey = minioManager.generateDocumentObjectKey(document.extension());
        minioManager.uploadFile(objectKey, document.content(), document.contentType());

        try {
            FileObject fileObject = FileObject.createOwnedPrivateDocument(
                    ownerMemberId,
                    category,
                    objectKey,
                    minioManager.createObjectUri(objectKey),
                    document.contentType(),
                    document.content().length,
                    minioManager.calculateChecksum(document.content())
            );
            return fileObjectRepository.saveAndFlush(fileObject);
        } catch (RuntimeException exception) {
            minioManager.deleteFileQuietly(objectKey);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public String createPrivateDownloadUrl(Long fileId) {
        FileObject file = fileObjectRepository.findById(fileId)
                .orElseThrow(() -> new FileManagedException("신청 서류 파일을 찾을 수 없습니다."));
        if (file.getVisibility() != FileVisibility.PRIVATE) {
            throw new FileManagedException("비공개 파일이 아닙니다.");
        }
        return minioManager.createPresignedGetUrl(file.getStorageKey(), PRIVATE_URL_EXPIRY_SECONDS);
    }

    private boolean hasExpectedSignature(String contentType, byte[] content) {
        if ("application/pdf".equals(contentType)) {
            return startsWith(content, new byte[]{'%', 'P', 'D', 'F', '-'});
        }
        if ("image/png".equals(contentType)) {
            return startsWith(content, new byte[]{
                    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
            });
        }
        return startsWith(content, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
    }

    private boolean startsWith(byte[] content, byte[] signature) {
        return content.length >= signature.length
                && Arrays.equals(Arrays.copyOf(content, signature.length), signature);
    }

    private void validateKakaoProfileUri(URI uri) {
        String host = uri.getHost();
        if (host == null
                || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || !(host.equals("kakaocdn.net") || host.endsWith(".kakaocdn.net"))) {
            throw new FileManagedException("허용하지 않는 카카오 프로필 이미지 주소입니다.");
        }
    }

    private byte[] readLimited(InputStream inputStream) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            total += read;
            if (total > MAX_SOCIAL_PROFILE_SIZE_BYTES) {
                throw new FileManagedException("카카오 프로필 이미지가 10MB를 초과합니다.");
            }
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
    }
}
