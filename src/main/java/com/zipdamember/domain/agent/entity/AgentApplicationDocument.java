package com.zipdamember.domain.agent.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import com.zipdamember.domain.agent.constant.AgentApplicationDocumentType;
import com.zipdamember.domain.agent.constant.DocumentOcrStatus;
import com.zipdamember.domain.agent.ocr.DocumentOcrResult;
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

import java.time.LocalDate;
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

    @Enumerated(EnumType.STRING)
    @Column(
            name = "ocr_status",
            nullable = false,
            length = 20,
            columnDefinition = "VARCHAR(20) DEFAULT 'PENDING'"
    )
    private DocumentOcrStatus ocrStatus = DocumentOcrStatus.PENDING;

    @Column(name = "ocr_text", columnDefinition = "MEDIUMTEXT")
    private String ocrText;

    @Column(name = "ocr_business_no", length = 50)
    private String ocrBusinessNo;

    @Column(name = "ocr_start_date")
    private LocalDate ocrStartDate;

    @Column(name = "ocr_representative_name", length = 50)
    private String ocrRepresentativeName;

    @Column(name = "ocr_agent_registration_no", length = 20)
    private String ocrAgentRegistrationNo;

    @Column(name = "ocr_agency_name", length = 50)
    private String ocrAgencyName;

    @Column(name = "ocr_completed_at")
    private LocalDateTime ocrCompletedAt;

    @Column(name = "ocr_failure_reason", length = 500)
    private String ocrFailureReason;

    @CreatedDate
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static AgentApplicationDocument create(Long applicationId, Long fileId,
                                                   AgentApplicationDocumentType documentType,
                                                   String checksum) {
        AgentApplicationDocument document = new AgentApplicationDocument();
        document.applicationId = applicationId;
        document.fileId = fileId;
        document.documentType = documentType;
        document.checksum = checksum;
        document.ocrStatus = DocumentOcrStatus.PENDING;
        return document;
    }

    public void completeOcr(DocumentOcrResult result, LocalDateTime now) {
        ocrStatus = DocumentOcrStatus.COMPLETED;
        ocrText = result.rawText();
        ocrBusinessNo = result.businessRegistrationNo();
        ocrStartDate = result.startDate();
        ocrRepresentativeName = result.representativeName();
        ocrAgentRegistrationNo = result.agentRegistrationNo();
        ocrAgencyName = result.agencyName();
        ocrCompletedAt = now;
        ocrFailureReason = null;
    }

    public void failOcr(String reason, LocalDateTime now) {
        ocrStatus = DocumentOcrStatus.FAILED;
        ocrCompletedAt = now;
        ocrFailureReason = reason == null
                ? null
                : reason.substring(0, Math.min(reason.length(), 500));
    }

    public boolean isOcrCompleted() {
        return ocrStatus == DocumentOcrStatus.COMPLETED;
    }

    public DocumentOcrResult toOcrResult() {
        return new DocumentOcrResult(
                ocrText,
                ocrBusinessNo,
                ocrStartDate,
                ocrRepresentativeName,
                ocrAgentRegistrationNo,
                ocrAgencyName
        );
    }

    @PrePersist
    private void generateDocumentId() {
        if (documentId == null) {
            documentId = TsidCreator.getTsid().toLong();
        }
    }
}
