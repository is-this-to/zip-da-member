package com.zipdamember.domain.member.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import com.zipdamember.domain.member.constant.MemberSanctionScope;
import com.zipdamember.domain.member.constant.MemberSuspensionReasonCode;
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

@Getter
@Entity
@Table(name = "member_sanction")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberSanction {

    @Id
    @Column(
            name = "sanction_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BIGINT UNSIGNED"
    )
    private Long sanctionId;

    @Column(name = "member_id", nullable = false, updatable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, updatable = false, length = 30)
    private MemberSanctionScope scope;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, updatable = false, length = 40)
    private MemberSuspensionReasonCode reasonCode;

    @Column(name = "related_report_id", updatable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long relatedReportId;

    @Column(name = "start_at", nullable = false, updatable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", updatable = false)
    private LocalDateTime endAt;

    @Column(name = "admin_id", nullable = false, updatable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long adminId;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "admin_released_by", columnDefinition = "BIGINT UNSIGNED")
    private Long adminReleasedBy;

    public static MemberSanction create(
            Long memberId,
            MemberSanctionScope scope,
            MemberSuspensionReasonCode reasonCode,
            Long relatedReportId,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Long adminId
    ) {
        MemberSanction sanction = new MemberSanction();
        sanction.memberId = memberId;
        sanction.scope = scope;
        sanction.reasonCode = reasonCode;
        sanction.relatedReportId = relatedReportId;
        sanction.startAt = startAt;
        sanction.endAt = endAt;
        sanction.adminId = adminId;
        return sanction;
    }

    public void release(Long adminReleasedBy, LocalDateTime releasedAt) {
        if (this.releasedAt != null) {
            throw new IllegalStateException("이미 해제된 회원 제재입니다.");
        }
        if (adminReleasedBy == null || adminReleasedBy <= 0) {
            throw new IllegalArgumentException("제재 해제 관리자 아이디는 필수입니다.");
        }
        if (releasedAt == null) {
            throw new IllegalArgumentException("제재 해제일시는 필수입니다.");
        }

        this.releasedAt = releasedAt;
        this.adminReleasedBy = adminReleasedBy;
    }

    public boolean isActiveAt(LocalDateTime baseAt) {
        return !startAt.isAfter(baseAt)
                && releasedAt == null
                && (endAt == null || endAt.isAfter(baseAt));
    }

    @PrePersist
    private void generateSanctionId() {
        if (sanctionId == null) {
            sanctionId = TsidCreator.getTsid().toLong();
        }
    }
}
