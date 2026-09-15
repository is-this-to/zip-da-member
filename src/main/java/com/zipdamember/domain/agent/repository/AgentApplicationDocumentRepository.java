package com.zipdamember.domain.agent.repository;

import com.zipdamember.domain.agent.entity.AgentApplicationDocument;
import com.zipdamember.domain.agent.constant.AgentApplicationDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentApplicationDocumentRepository
        extends JpaRepository<AgentApplicationDocument, Long> {

    List<AgentApplicationDocument> findAllByApplicationIdOrderByUploadedAtDescDocumentIdDesc(
            Long applicationId
    );

    boolean existsByApplicationIdAndDocumentType(
            Long applicationId,
            AgentApplicationDocumentType documentType
    );

    Optional<AgentApplicationDocument>
    findTopByApplicationIdAndDocumentTypeOrderByUploadedAtDescDocumentIdDesc(
            Long applicationId,
            AgentApplicationDocumentType documentType
    );

    Optional<AgentApplicationDocument> findByDocumentIdAndApplicationId(
            Long documentId, Long applicationId
    );
}
