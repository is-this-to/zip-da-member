package com.zipdamember.global.jpa.tsid;

import io.hypersistence.tsid.TSID;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;

import java.util.EnumSet;

public final class TsidIdentifierGenerator implements BeforeExecutionGenerator {

    @Override
    public Long generate(
            SharedSessionContractImplementor session,
            Object owner,
            Object currentValue,
            EventType eventType
    ) {
        return TSID.Factory.getTsid().toLong();
    }

    @Override
    public EnumSet<EventType> getEventTypes() {
        return EventTypeSets.INSERT_ONLY;
    }
}
