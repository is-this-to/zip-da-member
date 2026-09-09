package com.zipdamember.domain.agent.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import com.zipdamember.domain.agent.constant.AgentOperatingStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "agent_profile")
@SQLDelete(sql = "UPDATE agent_profile SET deleted_at = CURRENT_TIMESTAMP WHERE agent_id = ?")
@Filter(name = "softDelete", condition = "deleted_at IS NULL")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentProfile {

    @Id
    @Column(name = "agent_id", nullable = false, updatable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long agentId;

    @Column(name = "member_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long memberId;

    @Column(name = "intro", columnDefinition = "TEXT")
    private String intro;

    @Column(name = "profile_file_id", columnDefinition = "BIGINT UNSIGNED")
    private Long profileFileId;

    @Column(name = "approved_at", nullable = false)
    private LocalDateTime approvedAt;

    @Column(name = "business_registration_no", nullable = false, length = 50)
    private String businessRegistrationNo;

    @Column(name = "agency_name", nullable = false, length = 150)
    private String agencyName;

    @Column(name = "representative_name", nullable = false, length = 50)
    private String representativeName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address", length = 255)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "operating_status", nullable = false, length = 20)
    private AgentOperatingStatus operatingStatus = AgentOperatingStatus.ACTIVE;

    @Column(name = "status_changed_by", columnDefinition = "BIGINT UNSIGNED")
    private Long statusChangedBy;

    @Column(name = "status_changed_at")
    private LocalDateTime statusChangedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public AgentProfile(
            Long memberId,
            String businessRegistrationNo,
            String agencyName,
            String representativeName,
            String phone,
            String address,
            LocalDateTime approvedAt
    ) {
        this.memberId = memberId;
        this.businessRegistrationNo = businessRegistrationNo;
        this.agencyName = agencyName;
        this.representativeName = representativeName;
        this.phone = phone;
        this.address = address;
        this.approvedAt = approvedAt;
    }

    public void changeOperatingStatus(
            AgentOperatingStatus operatingStatus,
            Long statusChangedBy
    ) {
        if (operatingStatus == null) {
            throw new IllegalArgumentException("중개소 영업 상태는 필수입니다.");
        }
        if (statusChangedBy == null || statusChangedBy <= 0) {
            throw new IllegalArgumentException("상태 변경 관리자 ID는 필수입니다.");
        }

        this.operatingStatus = operatingStatus;
        this.statusChangedBy = statusChangedBy;
        this.statusChangedAt = LocalDateTime.now();
    }

    @PrePersist
    private void generateAgentId() {
        if (agentId == null) {
            agentId = TsidCreator.getTsid().toLong();
        }
        if (operatingStatus == null) {
            operatingStatus = AgentOperatingStatus.ACTIVE;
        }
    }
}
