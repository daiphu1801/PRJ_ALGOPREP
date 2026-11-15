package com.algoprep.identity.application.command;

import com.algoprep.identity.application.ports.out.EmailPort;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Audit row for a stored password, plus the security notice when it was the first password. The
 * notice is best effort: a mail failure is logged and reported to the caller, never rolled back.
 */
@Component
class PasswordChangeRecorder {
  private static final Logger LOG = LoggerFactory.getLogger(PasswordChangeRecorder.class);

  private final AuditLogPort audit;
  private final EmailPort email;

  PasswordChangeRecorder(AuditLogPort audit, EmailPort email) {
    this.audit = audit;
    this.email = email;
  }

  /** Returns true when the notice e-mail was due but could not be sent. */
  boolean record(UUID userId, String recipient, boolean firstPassword) {
    audit.record(new AuditEntry(userId, "PASSWORD_CHANGED", "USER", userId.toString(), null, null));
    if (!firstPassword) {
      return false;
    }
    try {
      email.send(
          recipient,
          "AlgoPrep - Tài khoản vừa được đặt mật khẩu",
          "Tài khoản "
              + recipient
              + " vừa được đặt mật khẩu. Nếu không phải bạn, hãy dùng chức năng quên mật khẩu"
              + " và liên hệ quản trị viên.");
      return false;
    } catch (RuntimeException e) {
      LOG.warn("First-password notice e-mail failed for user {}", userId, e);
      return true;
    }
  }
}
