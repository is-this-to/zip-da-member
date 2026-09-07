package com.zipdamember.domain.term.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.SQLDelete;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "term")
@SQLDelete(sql = "UPDATE term SET status = false WHERE term_id = ?")
@FilterDef(name = "softDelete")
@Filter(name = "softDelete", condition = "status = true")
@Getter
@Setter
public class Term {
    @Id
    @Column(name = "term_id", columnDefinition = "BIGINT UNSIGNED")
    private Long termId;

    @Column(name = "term_type", length = 30)
    private String termType;

    @Column(name = "term_version", length = 10)
    private String termVersion;

    @Column(name = "title", length = 100)
    private String title;

    @Column(name = "content", length = 2000)
    private String content;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @Column(name = "status", nullable = false)
    private Boolean status = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void generateTermId() {
        if(termId == null) {
            termId = TsidCreator.getTsid().toLong();
        }
    }
}