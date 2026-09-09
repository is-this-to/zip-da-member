package com.zipdamember.domain.member.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import com.zipdamember.domain.member.constant.MemberStatus;
import com.zipdamember.global.security.constant.MemberRolePolicy;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.SQLDelete;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "member_account")
@SQLDelete(sql = "UPDATE member_account SET status = 'WITHDRAWN', withdrawn_at = CURRENT_TIMESTAMP WHERE member_id = ?")
@FilterDef(name = "softDelete")
@Filter(name = "softDelete", condition = "withdrawn_at IS NULL")
@Getter
@Setter
public class MemberAccount {
    @Id
    @Column(name = "member_id", nullable = false, updatable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long memberId;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    @Email(message = "이메일 형식이 맞아야 합니다.")
    private String email;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "nickname", nullable = false, unique = true, length = 50)
    @Size(min = 2, max = 10, message = "닉네임은 2~10자여야 합니다.")
    @Pattern(regexp = "^[가-힣a-zA-Z0-9_]+${2,10}", message = "닉네임은 한글, 영문, 숫자, 밑줄만 사용할 수 있습니다.")
    private String nickname;

    @Column(name = "phone", nullable = false, length = 20)
    @Pattern(regexp = "^01[016789]\\d{7,8}$", message = "올바른 휴대전화 번호 형식이 아닙니다.")
    private String phone;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MemberStatus status = MemberStatus.ACTIVE;

    @Column(name = "profile_file_id", columnDefinition = "BIGINT UNSIGNED")
    private Long profileFileId;

    @Column(name = "member_role", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private MemberRolePolicy memberRole = MemberRolePolicy.USER;

    @Column(name = "email_verification_at")
    private LocalDateTime emailVerificationAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    private void generateMemberId() {
        if (memberId == null) {
            memberId = TsidCreator.getTsid().toLong();
        }
    }
}
