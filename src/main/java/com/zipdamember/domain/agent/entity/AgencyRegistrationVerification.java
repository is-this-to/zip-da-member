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

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "agency_registration_verification")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgencyRegistrationVerification {

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
    private VerificationProvider provider = VerificationProvider.MOLIT;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_type", nullable = false, length = 30)
    private VerificationType verificationType = VerificationType.STATUS;

    @Column(name = "request_agency_registration_no", length = 20)
    private String requestAgencyRegistrationNo;

    @Column(name = "agency_name", length = 50)
    private String agencyName;

    @Column(name = "request_representative_name", length = 50)
    private String requestRepresentativeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 20)
    private VerificationResultStatus resultStatus;

    @Column(name = "business_status", length = 20)
    private String businessStatus;

    @Column(name = "jibun_address", length = 255)
    private String jibunAddress;

    @Column(name = "road_address", length = 255)
    private String roadAddress;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    @Column(name = "raw_response_ref", length = 255)
    private String rawResponseRef;
}
