package com.zipdamember.domain.agent.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "agent_specialty", uniqueConstraints = @UniqueConstraint(columnNames = {"agent_id", "region_code"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentSpecialty {
    @Id
    @Column(name = "specialty_id", nullable = false, updatable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long specialtyId;
    @Column(name = "agent_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long agentId;
    @Column(name = "region_code", nullable = false, length = 20)
    private String regionCode;
    @Column(name = "region_name", nullable = false, length = 100)
    private String regionName;
    @Column(name = "display_order", nullable = false, columnDefinition = "INT UNSIGNED")
    private int displayOrder;

    public static AgentSpecialty create(Long agentId, String regionCode, String regionName, int displayOrder) {
        AgentSpecialty specialty = new AgentSpecialty();
        specialty.agentId = agentId;
        specialty.regionCode = regionCode;
        specialty.regionName = regionName;
        specialty.displayOrder = displayOrder;
        return specialty;
    }

    @PrePersist
    private void generateId() {
        if (specialtyId == null) specialtyId = TsidCreator.getTsid().toLong();
    }
}
