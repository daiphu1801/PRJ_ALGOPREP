package com.algoprep.identity.application.command;

import com.algoprep.common.exception.RateLimitExceededException;
import com.algoprep.identity.application.ports.out.EmailChangeOtpPort;
import com.algoprep.identity.application.ports.out.EmailPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Code delivery, matching and transaction cleanup for #11 and #12. Never logs a code or address.
 */
@Service
class EmailChangeService {
  private static final Logger LOG = LoggerFactory.getLogger(EmailChangeService.class);
  private final SecureRandom random = new SecureRandom();
  private final EmailChangeOtpPort otp;
  private final EmailPort mail;

  EmailChangeService(EmailChangeOtpPort otp, EmailPort mail) {
    this.otp = otp;
    this.mail = mail;
  }

  /**
   * Issues a code for {@code newEmail} and mails it there; the code is dropped if the caller rolls
   * back.
   */
  void send(UUID userId, String newEmail) {
    String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
    String digest = digest(code);
    switch (otp.issue(userId, newEmail, digest)) {
      case COOLDOWN ->
          throw new RateLimitExceededException(
              "IDT-V12", "Please wait before requesting another code");
      case BUDGET_EXCEEDED ->
          throw new RateLimitExceededException(
              "IDT-V05", "Too many e-mail change requests in 24 hours");
      case ISSUED -> {
        // Falls through to delivery below.
      }
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED) {
              clearSafely(userId, digest);
            }
          }
        });
    mail.send(
        newEmail,
        "AlgoPrep - Xác nhận email mới",
        "Mã xác nhận đổi email của bạn: " + code + ". Mã có hiệu lực trong 10 phút.");
  }

  /** The pending new address when {@code code} is the live one; a wrong code counts a failure. */
  Optional<String> match(UUID userId, String code) {
    return otp.match(userId, digest(code));
  }

  void clearAfterCommit(UUID userId, String code) {
    String digest = digest(code);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            clearSafely(userId, digest);
          }
        });
  }

  private void clearSafely(UUID userId, String digest) {
    try {
      otp.clear(userId, digest);
    } catch (RuntimeException ex) {
      LOG.warn("E-mail change cleanup unavailable; the original expiry is retained");
    }
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
