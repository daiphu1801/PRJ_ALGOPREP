package com.algoprep.identity.application.command;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.identity.application.dto.UpdateLogRetentionCommand;
import com.algoprep.identity.application.ports.in.UpdateLogRetentionSettingsUseCase;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.identity.domain.model.ExpiredLogPolicy;
import com.algoprep.identity.domain.model.LogRetentionSettings;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import com.algoprep.identity.domain.ports.out.LogRetentionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * #52. Validates, stores both keys and writes one audit row with the old and new values in the same
 * transaction (BD 1.13). The Function gate {@code SYSTEM_AUDIT_LOG} is on the controller.
 */
@Service
class UpdateLogRetentionSettingsCommandHandler implements UpdateLogRetentionSettingsUseCase {
  static final String ACTION = "AUDIT_LOG_SETTINGS_CHANGED";
  static final String TARGET_TYPE = "AUDIT_LOG_SETTINGS";

  /** Upper bound so the cut-off date stays inside the database timestamp range. */
  static final long MAX_DAYS = 1_000_000L;

  private final LogRetentionRepository retention;
  private final AuditLogPort audit;

  UpdateLogRetentionSettingsCommandHandler(LogRetentionRepository retention, AuditLogPort audit) {
    this.retention = retention;
    this.audit = audit;
  }

  @Override
  @Transactional
  public LogRetentionSettings update(UpdateLogRetentionCommand command) {
    LogRetentionSettings next = validate(command);
    LogRetentionSettings before = retention.readForUpdate();
    retention.save(next, command.actorUserId());
    audit.record(
        new AuditEntry(
            command.actorUserId(), ACTION, TARGET_TYPE, "audit_log", json(before), json(next)));
    return next;
  }

  private static LogRetentionSettings validate(UpdateLogRetentionCommand command) {
    Long days = command.retentionDays();
    if (days == null || days < 1 || days > MAX_DAYS) {
      throw invalid("Retention must be a whole number of days of at least 1");
    }
    return new LogRetentionSettings(days.intValue(), policy(command.expiredPolicy()));
  }

  private static ExpiredLogPolicy policy(String sent) {
    for (ExpiredLogPolicy candidate : ExpiredLogPolicy.values()) {
      if (candidate.name().equals(sent)) {
        return candidate;
      }
    }
    throw invalid("Expired log policy must be DELETE or ARCHIVE");
  }

  private static DomainRuleViolationException invalid(String message) {
    return new DomainRuleViolationException(IdentityErrorCodes.LOG_RETENTION_INVALID, message);
  }

  private static String json(LogRetentionSettings s) {
    return "{\"retentionDays\":"
        + s.retentionDays()
        + ",\"expiredPolicy\":\""
        + s.expiredPolicy()
        + "\"}";
  }
}
