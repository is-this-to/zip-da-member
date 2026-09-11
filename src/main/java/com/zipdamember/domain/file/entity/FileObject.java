package com.zipdamember.domain.file.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import com.zipdamember.domain.file.constant.FileCategory;
import com.zipdamember.domain.file.constant.FileVisibility;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_object")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileObject {

    @Id
    @Column(name = "file_id", nullable = false, updatable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long fileId;

    @Column(name = "owner_member_id", columnDefinition = "BIGINT UNSIGNED")
    private Long ownerMemberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private FileCategory category;

    @Column(name = "storage_key", nullable = false, unique = true, length = 255)
    private String storageKey;

    @Column(name = "file_uri", length = 512)
    private String fileUri;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private FileVisibility visibility;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "checksum", length = 64)
    private String checksum;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    private FileObject(
            FileCategory category,
            String storageKey,
            String fileUri,
            FileVisibility visibility,
            String contentType,
            long sizeBytes,
            String checksum,
            LocalDateTime expiresAt
    ) {
        this.category = category;
        this.storageKey = storageKey;
        this.fileUri = fileUri;
        this.visibility = visibility;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.checksum = checksum;
        this.expiresAt = expiresAt;
    }

    public static FileObject createTemporaryProfile(
            String storageKey,
            String fileUri,
            String contentType,
            long sizeBytes,
            String checksum,
            LocalDateTime expiresAt
    ) {
        return new FileObject(
                FileCategory.PROFILE,
                storageKey,
                fileUri,
                FileVisibility.PUBLIC,
                contentType,
                sizeBytes,
                checksum,
                expiresAt
        );
    }

    public static FileObject createOwnedProfile(
            Long ownerMemberId,
            String storageKey,
            String fileUri,
            String contentType,
            long sizeBytes,
            String checksum
    ) {
        FileObject fileObject = new FileObject(
                FileCategory.PROFILE,
                storageKey,
                fileUri,
                FileVisibility.PUBLIC,
                contentType,
                sizeBytes,
                checksum,
                null
        );
        fileObject.ownerMemberId = ownerMemberId;
        return fileObject;
    }

    public static FileObject createOwnedPrivateDocument(
            Long ownerMemberId,
            FileCategory category,
            String storageKey,
            String fileUri,
            String contentType,
            long sizeBytes,
            String checksum
    ) {
        if (category != FileCategory.BUSINESS_LICENSE
                && category != FileCategory.AGENT_CERTIFICATE) {
            throw new IllegalArgumentException("중개사 신청 서류 카테고리가 아닙니다.");
        }

        FileObject fileObject = new FileObject(
                category,
                storageKey,
                fileUri,
                FileVisibility.PRIVATE,
                contentType,
                sizeBytes,
                checksum,
                null
        );
        fileObject.ownerMemberId = ownerMemberId;
        return fileObject;
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    public boolean isOwned() {
        return ownerMemberId != null;
    }

    public void assignOwner(Long memberId) {
        this.ownerMemberId = memberId;
        this.expiresAt = null;
    }

    @PrePersist
    private void generateFileId() {
        if (fileId == null) {
            fileId = TsidCreator.getTsid().toLong();
        }
    }
}
