package com.algoprep.identity.domain.model;

import java.util.List;

/**
 * Structured class schedule stored as jsonb in {@code classes.schedule} (DD validation F1 section
 * 5). Every part is optional. Times are {@code HH:mm}, dates {@code yyyy-MM-dd}, days are 1 to 7.
 */
public record ScheduleSpec(
    List<Integer> days,
    String startTime,
    String endTime,
    String room,
    String startDate,
    String endDate,
    String note) {}
