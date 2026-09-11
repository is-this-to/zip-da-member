package com.zipdamember.domain.agent.entity;

import com.zipdamember.domain.agent.constant.AgentBusinessDay;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AgentBusinessDayConverter implements AttributeConverter<AgentBusinessDay, Integer> {
    @Override
    public Integer convertToDatabaseColumn(AgentBusinessDay attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public AgentBusinessDay convertToEntityAttribute(Integer value) {
        return value == null ? null : AgentBusinessDay.fromValue(value);
    }
}
