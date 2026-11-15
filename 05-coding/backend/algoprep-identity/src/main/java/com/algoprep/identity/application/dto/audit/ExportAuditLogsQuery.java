package com.algoprep.identity.application.dto;

import java.util.List;
import java.util.UUID;

/**
 * Raw, unvalidated parameters of #40.
 *
 * @param actorUserId the admin exporting; the export is itself logged under this id
 * @param q keyword
 * @param categoryCode display group or {@code ALL}
 * @param dateFrom first local (UTC+7) day
 * @param dateTo last local (UTC+7) day
 * @param columns requested columns from {@code time|actor|service|category|content|id}
 * @param format must be {@code csv} when present
 * @param acceptLanguage raw {@code Accept-Language} header, {@code vi} unless it starts with {@code
 *     en}
 */
public record ExportAuditLogsQuery(
    UUID actorUserId,
    String q,
    String categoryCode,
    String dateFrom,
    String dateTo,
    List<String> columns,
    String format,
    String acceptLanguage) {}
