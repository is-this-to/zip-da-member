package com.zipdamember.domain.agent.request;

import com.zipdamember.domain.agent.constant.AgentBusinessDay;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record AgentBusinessHourRequest(
        @NotNull AgentBusinessDay dayOfWeek,
        LocalTime openTime,
        LocalTime closeTime,
        boolean closed
) {
}
