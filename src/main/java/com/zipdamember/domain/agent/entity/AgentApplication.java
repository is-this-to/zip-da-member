package com.zipdamember.domain.agent.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import com.zipdamember.domain.agent.constant.AgentApplicationStatus;
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
}
