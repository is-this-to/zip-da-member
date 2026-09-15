package com.zipdamember.domain.agent.response;

import com.zipdamember.domain.agent.constant.AgentBusinessDay;
import com.zipdamember.domain.agent.entity.AgentBusinessHour;

import java.time.LocalTime;

public record AgentBusinessHourResponse(
        AgentBusinessDay dayOfWeek,
        LocalTime openTime,
        LocalTime closeTime,
        boolean closed
) {
    public static AgentBusinessHourResponse from(AgentBusinessHour hour) {
        return new AgentBusinessHourResponse(
                hour.getDayOfWeek(),
                hour.getOpenTime(),
                hour.getCloseTime(),
                hour.isClosed()
        );
    }
}
