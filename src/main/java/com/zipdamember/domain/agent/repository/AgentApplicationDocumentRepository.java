package com.zipdamember.domain.agent.repository;

import com.zipdamember.domain.agent.entity.AgentApplicationDocument;
import com.zipdamember.domain.agent.constant.AgentApplicationDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentApplicationDocumentRepository
        extends JpaRepository<AgentApplicationDocument, Long> {

    List<AgentApplicationDocument> findAllByApplicationIdOrderByUploadedAtDescDocumentIdDesc(
            Long applicationId
    );

    boolean existsByApplicationIdAndDocumentType(
            Long applicationId,
            AgentApplicationDocumentType documentType
    );
}
