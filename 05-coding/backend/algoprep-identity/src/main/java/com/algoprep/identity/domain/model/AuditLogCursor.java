package com.algoprep.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * A position in the log order {@code (created_at DESC, id DESC)}, so paging survives rows that were
 * purged since the previous page (DD 0.7.2).
 *
 * @param createdAt time of the row
 * @param id id of the row
 */
public record AuditLogCursor(Instant createdAt, UUID id) {}
