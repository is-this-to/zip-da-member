package com.zipdamember.domain.agent.entity;

import com.zipdamember.domain.agent.constant.VerificationProvider;
import com.zipdamember.domain.agent.constant.VerificationResultStatus;
import com.zipdamember.domain.agent.constant.VerificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "business_verification")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BusinessVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
            name = "verification_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BIGINT UNSIGNED"
    )
    private Long verificationId;

    @Column(name = "application_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 30)
    private VerificationProvider provider = VerificationProvider.NTS;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_type", nullable = false, length = 30)
    private VerificationType verificationType = VerificationType.STATUS;

    @Column(name = "request_business_no", length = 50)
    private String requestBusinessNo;

    @Column(name = "request_start_date")
    private LocalDate requestStartDate;

    @Column(name = "request_representative_name", length = 50)
    private String requestRepresentativeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 20)
    private VerificationResultStatus resultStatus;

    @Column(name = "business_status", length = 20)
    private String businessStatus;

    @Column(name = "tax_type", length = 30)
    private String taxType;

    @Column(name = "closed_at")
    private LocalDate closedAt;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    @Column(name = "raw_response_ref", length = 255)
    private String rawResponseRef;
}
