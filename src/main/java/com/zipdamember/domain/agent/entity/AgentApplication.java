package com.zipdamember.domain.agent.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import com.zipdamember.domain.agent.constant.AgentApplicationStatus;
import com.zipdamember.domain.agent.constant.VerificationResultStatus;
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
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "agent_application")
@SQLDelete(sql = "UPDATE agent_application SET deleted_at = CURRENT_TIMESTAMP WHERE application_id = ?")
@Filter(name = "softDelete", condition = "deleted_at IS NULL")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentApplication {

    @Id
    @Column(
            name = "application_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BIGINT UNSIGNED"
    )
    private Long applicationId;

    @Column(name = "member_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AgentApplicationStatus status = AgentApplicationStatus.PENDING;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @Column(name = "supplement_deadline")
    private LocalDateTime supplementDeadline;

    @Column(name = "request_business_no", length = 50)
    private String requestBusinessNo;

    @Column(name = "request_agency_registration_no", length = 20)
    private String requestAgencyRegistrationNo;

    @Column(name = "request_agency_name", length = 150)
    private String requestAgencyName;

    @Column(name = "request_start_date")
    private LocalDate requestStartDate;

    @Column(name = "request_representative_name", length = 50)
    private String requestRepresentativeName;

    @Column(name = "reviewer_admin_id", columnDefinition = "BIGINT UNSIGNED")
    private Long reviewerAdminId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    private void generateApplicationId() {
        if (applicationId == null) {
            applicationId = TsidCreator.getTsid().toLong();
        }
        if (status == null) {
            status = AgentApplicationStatus.PENDING;
        }
    }

    public void requestSupplement(
            String supplementReason,
            LocalDateTime supplementDeadline,
            Long reviewerAdminId
    ) {
        if (status != AgentApplicationStatus.UNDER_REVIEW) {
            throw new IllegalStateException("심사 중 상태의 신청만 보완 요청할 수 있습니다.");
        }
        if (supplementReason == null || supplementReason.isBlank()) {
            throw new IllegalArgumentException("보완 요청 사유는 필수입니다.");
        }
        if (supplementDeadline == null || !supplementDeadline.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("보완 마감일은 현재 이후여야 합니다.");
        }
        if (reviewerAdminId == null || reviewerAdminId <= 0) {
            throw new IllegalArgumentException("심사 관리자 아이디는 필수입니다.");
        }

        status = AgentApplicationStatus.REJECTED;
        rejectReason = supplementReason;
        this.supplementDeadline = supplementDeadline;
        this.reviewerAdminId = reviewerAdminId;
    }

    public void approve(Long reviewerAdminId) {
        if (status != AgentApplicationStatus.UNDER_REVIEW) {
            throw new IllegalStateException("심사 중 상태의 신청만 승인할 수 있습니다.");
        }
        if (reviewerAdminId == null || reviewerAdminId <= 0) {
            throw new IllegalArgumentException("심사 관리자 아이디는 필수입니다.");
        }

        status = AgentApplicationStatus.APPROVED;
        this.reviewerAdminId = reviewerAdminId;
    }

    public AgentApplicationStatus applyVerificationResults(
            VerificationResultStatus businessVerificationResult,
            VerificationResultStatus agencyRegistrationVerificationResult
    ) {
        if (status != AgentApplicationStatus.PENDING) {
            throw new IllegalStateException("대기 상태의 신청만 검증 결과를 반영할 수 있습니다.");
        }

        if (businessVerificationResult == VerificationResultStatus.MISMATCHED
                || agencyRegistrationVerificationResult == VerificationResultStatus.MISMATCHED) {
            status = AgentApplicationStatus.INCORRECT_DATA;
            return status;
        }

        if (businessVerificationResult == VerificationResultStatus.ERROR
                || agencyRegistrationVerificationResult == VerificationResultStatus.ERROR) {
            return status;
        }

        status = AgentApplicationStatus.UNDER_REVIEW;
        return status;
    }
}
