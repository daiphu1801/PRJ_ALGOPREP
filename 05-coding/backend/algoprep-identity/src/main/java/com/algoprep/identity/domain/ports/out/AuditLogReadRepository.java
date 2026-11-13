package com.algoprep.identity.domain.ports.out;

import com.algoprep.identity.domain.model.ActiveAdmin;
import com.algoprep.identity.domain.model.AuditLogCursor;
import com.algoprep.identity.domain.model.AuditLogFilter;
import com.algoprep.identity.domain.model.AuditLogRow;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Read side of {@code identity.system_audit_logs} (#39, #40, #64, #65, #66). The write side is
 * {@link AuditLogPort}. Windows ("24 hours", "7 days", retention) use the database clock.
 */
public interface AuditLogReadRepository {

  /**
   * Rows matching the filter in {@code (created_at DESC, id DESC)} order, strictly after {@code
   * after} when it is given.
   */
  List<AuditLogRow> search(AuditLogFilter filter, AuditLogCursor after, int limit);

  /** Retention window in days from {@code system_settings}, 90 when the setting is missing. */
  int retentionDays();

  /** Up to five admins with actions in the last 24 hours, busiest first (#64). */
  List<ActiveAdmin> activeAdmins();

  /** Action counts of the last 7 days keyed by raw {@code action_type} (#65). */
  Map<String, Long> actionCountsLastSevenDays();

  /** Position of the newest row, empty when the log is empty. */
  Optional<AuditLogCursor> latest();

  /** Position of one row, empty when it does not exist or is older than the retention window. */
  Optional<AuditLogCursor> findWithinRetention(UUID id);

  /**
   * Rows strictly after {@code after} up to and including {@code upTo} (when given), oldest first;
   * the feed of the live stream (#66).
   */
  List<AuditLogRow> newer(AuditLogCursor after, AuditLogCursor upTo, int limit);
}
