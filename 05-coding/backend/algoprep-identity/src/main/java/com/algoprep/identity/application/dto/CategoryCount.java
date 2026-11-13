package com.algoprep.identity.application.dto;

/**
 * Actions of one display group in the last seven days (#65).
 *
 * @param categoryCode {@code AUTH}, {@code PERMISSION}, {@code CONFIG} or {@code CONTENT}
 * @param count number of actions, 0 when none
 */
public record CategoryCount(String categoryCode, long count) {}
