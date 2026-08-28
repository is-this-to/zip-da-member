package com.zipdamember.domain.member.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import com.zipdamember.global.security.constant.MemberRolePolicy;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.sql.Types;
import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "member_account")
@SQLDelete(sql = "UPDATE member_account SET withdrawn_at = NULL WHERE member_id = ?")
@FilterDef(name = "softDelete")
@Filter(name = "softDelete", condition = "withdrawn_at IS NULL")
@Getter
@Setter
public class MemberAccount {
    @Id
    @Column(name = "member_id", columnDefinition = "BIGINT UNSIGNED")
    private Long memberId;

    @Column(name = "email", length = 30)
    private String email;

    @Column(name = "password", length = 10)
    private String password;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "nickname", length = 2000)
    private String nickName;

    @Column(name = "phone", nullable = false)


    @Column(name = "status", nullable = false)

    @Column(name = "profile_file_id", nullable = false)


    @Column(name = "member_role", nullable = false)
    @Enumerated(value = EnumType.STRING)
    private MemberRolePolicy memberRole = MemberRolePolicy.USER;

    @Column(name = "email_verification_at", nullable = false)
    private LocalDateTime emailVerificationAt;

    @Column(name = "withdrawn_at", nullable = false)
    private LocalDateTime withdrawnAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    private void generateTermId() {
        if(termId == null) {
            termId = TsidCreator.getTsid().toLong();
        }
    }
}
