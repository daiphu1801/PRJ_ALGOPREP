package com.algoprep.identity.application.dto;

import com.algoprep.identity.domain.model.AuditLogCursor;
import java.util.List;

/**
 * Answer to one {@link AuditStreamRequest}.
 *
 * @param items new rows, oldest first, already masked
 * @param position position to continue from; null when the log is empty or {@code resync} is set
 * @param resync true when the reconnect marker is gone, so the client must reload the first page
 */
public record AuditStreamBatch(List<AuditLogItem> items, AuditLogCursor position, boolean resync) {}
