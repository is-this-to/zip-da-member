package com.zipdamember.domain.agent.constant;

public enum AgentBusinessDay {
    MONDAY(1), TUESDAY(2), WEDNESDAY(3), THURSDAY(4),
    FRIDAY(5), SATURDAY(6), SUNDAY(7);

    private final int value;

    AgentBusinessDay(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static AgentBusinessDay fromValue(int value) {
        for (AgentBusinessDay day : values()) {
            if (day.value == value) return day;
        }
        throw new IllegalArgumentException("올바르지 않은 영업 요일입니다: " + value);
    }
}
