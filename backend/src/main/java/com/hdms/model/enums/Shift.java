package com.hdms.model.enums;

import java.time.LocalTime;

public enum Shift {
    MORNING(LocalTime.of(7, 0), LocalTime.of(12, 0)),
    AFTERNOON(LocalTime.of(13, 0), LocalTime.of(18, 0)),
    EVENING(LocalTime.of(18, 30), LocalTime.of(23, 0));

    private final LocalTime start;
    private final LocalTime end;

    Shift(LocalTime start, LocalTime end) {
        this.start = start;
        this.end = end;
    }

    public LocalTime getStart() { return start; }
    public LocalTime getEnd() { return end; }
}
