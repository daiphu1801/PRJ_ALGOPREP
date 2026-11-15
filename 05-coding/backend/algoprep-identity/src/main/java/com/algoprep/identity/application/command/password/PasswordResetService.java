package com.algoprep.identity.application.command;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.identity.application.ports.out.EmailPort;
import com.algoprep.identity.application.ports.out.PasswordResetOtpPort;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.identity.domain.model.PasswordResetAccount;
import com.algoprep.identity.domain.model.PasswordResetChallenge;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Delivery, non-consuming verification and cleanup for BR-04. No secret is logged. */
@Service
class PasswordResetService {
  private static final Logger LOG = LoggerFactory.getLogger(PasswordResetService.class);
  private static final UUID MISSING_USER = new UUID(0, 0);
  private final SecureRandom random = new SecureRandom();
  private final PasswordResetOtpPort otp;
  private final EmailPort mail;
  private final AuditLogPort audit;

  PasswordResetService(PasswordResetOtpPort otp, EmailPort mail, AuditLogPort audit) {
    this.otp = otp;
    this.mail = mail;
    this.audit = audit;
  }

  void send(PasswordResetAccount account) {
    UUID id = account.user().id();
    Instant issued = account.databaseTime();
    if (account.user().passwordHash() == null) {
      sendGuidance(account);
      return;
    }
    if (!account.canResetPassword()) {
      return;
    }
    String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
    if (!otp.issue(id, new PasswordResetChallenge(digest(code), issued))) {
      return;
    }
    clearOnRollback(id, issued);
    audit.record(new AuditEntry(id, "PASSWORD_RESET_REQUESTED", "USER", id.toString(), null, null));
    mail.send(
        account.email(),
        "AlgoPrep - Đặt lại mật khẩu",
        "Mã đặt lại mật khẩu của bạn: " + code + ". Mã có hiệu lực trong 10 phút.");
  }

  Instant check(PasswordResetAccount account, String code) {
    if (code == null || !code.matches("[0-9]{6}")) {
      throw invalid();
    }
    UUID userId = MISSING_USER;
    if (account != null && account.canResetPassword()) {
      userId = account.user().id();
    }
    // Even unknown/OAuth-only accounts perform the Redis step before the generic failure.
    Instant issued = otp.verify(userId, digest(code)).orElse(null);
    if (account == null
        || !account.canResetPassword()
        || issued == null
        || (account.passwordChangedAt() != null && !account.passwordChangedAt().isBefore(issued))) {
      throw invalid();
    }
    return issued;
  }

  void clearAfterCommit(UUID userId, Instant issued) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            clearSafely(userId, issued);
          }
        });
  }

  private void sendGuidance(PasswordResetAccount account) {
    UUID id = account.user().id();
    Instant issued = account.databaseTime();
    if (otp.reserveGuidance(id, issued)) {
      clearOnRollback(id, issued);
      mail.send(
          account.email(),
          "AlgoPrep - Hướng dẫn đăng nhập",
          "Tài khoản của bạn sử dụng đăng nhập qua nhà cung cấp bên ngoài. "
              + "Vui lòng đăng nhập bằng Google hoặc GitHub mà bạn đã liên kết với tài khoản.");
    }
  }

  private void clearOnRollback(UUID userId, Instant issued) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED) {
              clearSafely(userId, issued);
            }
          }
        });
  }

  private void clearSafely(UUID userId, Instant issued) {
    try {
      otp.clear(userId, issued);
    } catch (RuntimeException ex) {
      LOG.warn("Password reset cleanup unavailable; original expiry is retained");
    }
  }

  private static DomainRuleViolationException invalid() {
    return new DomainRuleViolationException("IDT-107", "Reset code is invalid or unavailable");
  }

  private static String digest(String code) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is required by Java", ex);
    }
  }
}
