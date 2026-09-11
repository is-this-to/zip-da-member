package com.zipdamember.domain.agent.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import com.zipdamember.domain.agent.constant.AgentBusinessDay;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@Entity
@Table(name = "agent_business_hour", uniqueConstraints = @UniqueConstraint(columnNames = {"agent_id", "day_of_week"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentBusinessHour {
    @Id
    @Column(name = "business_hour_id", nullable = false, updatable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long businessHourId;
    @Column(name = "agent_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long agentId;
    @Convert(converter = AgentBusinessDayConverter.class)
    @Column(name = "day_of_week", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    private AgentBusinessDay dayOfWeek;
    @Column(name = "open_time")
    private LocalTime openTime;
    @Column(name = "close_time")
    private LocalTime closeTime;
    @Column(name = "closed", nullable = false)
    private boolean closed;

    public static AgentBusinessHour create(
            Long agentId,
            AgentBusinessDay day,
            LocalTime openTime,
            LocalTime closeTime,
            boolean closed
    ) {
        if (!closed && (openTime == null || closeTime == null || !openTime.isBefore(closeTime))) {
            throw new IllegalArgumentException("영업일은 시작 시각이 종료 시각보다 빨라야 합니다.");
        }
        AgentBusinessHour hour = new AgentBusinessHour();
        hour.agentId = agentId;
        hour.dayOfWeek = day;
        hour.closed = closed;
        hour.openTime = closed ? null : openTime;
        hour.closeTime = closed ? null : closeTime;
        return hour;
    }

    @PrePersist
    private void generateId() {
        if (businessHourId == null) businessHourId = TsidCreator.getTsid().toLong();
    }
}
