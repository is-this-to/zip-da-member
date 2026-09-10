package com.zipdamember.domain.agent.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import com.zipdamember.domain.agent.constant.AgentApplicationDocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "agent_application_document")
@SQLDelete(sql = "UPDATE agent_application_document SET deleted_at = CURRENT_TIMESTAMP WHERE document_id = ?")
@Filter(name = "softDelete", condition = "deleted_at IS NULL")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentApplicationDocument {

    @Id
    @Column(
            name = "document_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BIGINT UNSIGNED"
    )
    private Long documentId;

    @Column(name = "application_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long applicationId;

    @Column(name = "file_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long fileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 40)
    private AgentApplicationDocumentType documentType;

    @Column(name = "checksum", nullable = false, length = 64)
    private String checksum;

    @CreatedDate
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    private void generateDocumentId() {
        if (documentId == null) {
            documentId = TsidCreator.getTsid().toLong();
        }
    }
}
