package com.zipdamember.domain.file.service;

import com.zipdamember.domain.file.entity.FileObject;
import com.zipdamember.domain.file.repository.FileObjectRepository;
import com.zipdamember.global.minio.MinioManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileServiceTest {

    private MinioManager minioManager;
    private FileObjectRepository fileObjectRepository;
    private FileService fileService;

    @BeforeEach
    void setUp() {
        minioManager = mock(MinioManager.class);
        fileObjectRepository = mock(FileObjectRepository.class);
        fileService = new FileService(minioManager, fileObjectRepository);
    }

    @Test
    void uploadProfile_savesMinioObjectAndFileMetadata() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.png",
                "image/png",
                new byte[]{1, 2, 3}
        );
        String objectKey = "profiles/20260908_profile.png";
        String fileUri = "http://localhost:9000/zipda/profiles/20260908_profile.png";

        when(minioManager.generateProfileObjectKey(file)).thenReturn(objectKey);
        when(minioManager.createObjectUri(objectKey)).thenReturn(fileUri);
        when(minioManager.calculateChecksum(file)).thenReturn("checksum");
        when(fileObjectRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(FileObject.class)))
                .thenAnswer(invocation -> {
                    FileObject saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "fileId", 100L);
                    return saved;
                });

        var response = fileService.uploadProfile(file);

        verify(minioManager).uploadFile(objectKey, file);
        ArgumentCaptor<FileObject> captor = ArgumentCaptor.forClass(FileObject.class);
        verify(fileObjectRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getCategory().name()).isEqualTo("PROFILE");
        assertThat(captor.getValue().getVisibility().name()).isEqualTo("PUBLIC");
        assertThat(captor.getValue().getOwnerMemberId()).isNull();
        assertThat(response.fileId()).isEqualTo("100");
        assertThat(response.fileUri()).isEqualTo(fileUri);
    }

    @Test
    void assignProfileToMember_setsOwnerAndClearsTemporaryExpiry() {
        FileObject profile = FileObject.createTemporaryProfile(
                "profiles/profile.png",
                "http://localhost/profile.png",
                "image/png",
                3L,
                "checksum",
                LocalDateTime.now().plusHours(1)
        );
        when(fileObjectRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(profile));

        fileService.assignProfileToMember(100L, 200L, LocalDateTime.now());

        assertThat(profile.getOwnerMemberId()).isEqualTo(200L);
        assertThat(profile.getExpiresAt()).isNull();
    }
}
