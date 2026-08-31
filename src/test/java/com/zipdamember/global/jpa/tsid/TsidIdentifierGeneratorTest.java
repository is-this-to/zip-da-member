package com.zipdamember.global.jpa.tsid;

import org.hibernate.generator.EventType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TsidIdentifierGeneratorTest {

    private final TsidIdentifierGenerator generator = new TsidIdentifierGenerator();

    @Test
    void generate_returnsPositiveLong() {
        Long generatedId = generator.generate(null, null, null, EventType.INSERT);

        assertThat(generatedId).isPositive();
    }

    @Test
    void generate_manyTimes_returnsUniqueValues() {
        int generationCount = 100_000;
        Set<Long> generatedIds = new HashSet<>(generationCount);

        for (int index = 0; index < generationCount; index++) {
            generatedIds.add(generator.generate(null, null, null, EventType.INSERT));
        }

        assertThat(generatedIds).hasSize(generationCount);
    }

    @Test
    void getEventTypes_returnsInsertOnly() {
        assertThat(generator.getEventTypes()).containsExactly(EventType.INSERT);
    }
}
