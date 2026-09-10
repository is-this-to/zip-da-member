package com.zipdamember.domain.file.repository;

import com.zipdamember.domain.file.entity.FileObject;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FileObjectRepository extends JpaRepository<FileObject, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select file from FileObject file where file.fileId = :fileId")
    Optional<FileObject> findByIdForUpdate(@Param("fileId") Long fileId);
}
