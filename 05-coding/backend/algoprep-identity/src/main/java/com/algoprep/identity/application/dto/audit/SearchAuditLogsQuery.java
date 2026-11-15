package com.algoprep.identity.application.dto;

/**
 * Raw, unvalidated parameters of #39; the handler validates them (DD validation section 12).
 *
 * @param q keyword
 * @param categoryCode {@code ALL}, {@code AUTH}, {@code PERMISSION}, {@code CONFIG} or {@code
 *     CONTENT}
 * @param dateFrom first local (UTC+7) day, {@code yyyy-MM-dd}
 * @param dateTo last local (UTC+7) day, {@code yyyy-MM-dd}
 * @param cursor opaque cursor from the previous page
 */
public record SearchAuditLogsQuery(
    String q, String categoryCode, String dateFrom, String dateTo, String cursor) {}
