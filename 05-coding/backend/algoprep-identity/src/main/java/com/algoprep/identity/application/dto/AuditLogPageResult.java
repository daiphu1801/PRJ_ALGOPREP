package com.algoprep.identity.application.dto;

import java.util.List;

/**
 * One page of #39.
 *
 * @param items up to 50 rows, newest first
 * @param nextCursor cursor of the next page, null on the last page
 */
public record AuditLogPageResult(List<AuditLogItem> items, String nextCursor) {}
