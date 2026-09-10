package com.zipdamember.domain.term.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "term_agreement",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_term_agreement_member_term",
                columnNames = {"member_id", "term_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TermAgreement {

    @Id
    @Column(name = "agreement_id", nullable = false, updatable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long agreementId;

    @Column(name = "member_id", nullable = false, updatable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long memberId;

    @Column(name = "term_id", nullable = false, updatable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long termId;

    @Column(name = "is_agreed", nullable = false)
    private Boolean agreed;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private TermAgreement(Long memberId, Long termId, Boolean agreed) {
        this.memberId = memberId;
        this.termId = termId;
        this.agreed = agreed;
    }

    public static TermAgreement create(Long memberId, Long termId, Boolean agreed) {
        return new TermAgreement(memberId, termId, agreed);
    }

    @PrePersist
    private void generateAgreementId() {
        if (agreementId == null) {
            agreementId = TsidCreator.getTsid().toLong();
        }
    }
}
