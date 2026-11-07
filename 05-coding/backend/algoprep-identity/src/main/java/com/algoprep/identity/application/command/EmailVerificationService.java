package com.algoprep.identity.application.command;

import com.algoprep.common.exception.RateLimitExceededException;
import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.identity.application.ports.out.EmailPort;
import com.algoprep.identity.application.ports.out.EmailVerificationOtpPort;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Coordinates delivery and transaction cleanup without logging OTP or recipient information. */
@Service
class EmailVerificationService {
  private static final Logger LOG = LoggerFactory.getLogger(EmailVerificationService.class);
  private final SecureRandom random = new SecureRandom();
  private final EmailVerificationOtpPort otp;
  private final EmailPort email;

  EmailVerificationService(EmailVerificationOtpPort otp, EmailPort email) {
    this.otp = otp;
    this.email = email;
  }

  void send(UUID userId, String recipient) {
    String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
    String digest = digest(code);
    if (!otp.issue(userId, digest)) {
      throw new RateLimitExceededException("IDT-V12", "Please wait before requesting another code");
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
    try {
      email.send(
          recipient,
          "AlgoPrep - Xác thực email",
          "Mã xác thực email của bạn: " + code + ". Mã có hiệu lực trong 10 phút.");
    } catch (RuntimeException failure) {
      // The cause is dropped on purpose: mail exceptions can carry the recipient address.
      LOG.warn("Verification e-mail could not be sent");
      throw new ServiceUnavailableException(
          IdentityErrorCodes.VERIFICATION_EMAIL_UNAVAILABLE,
          "Verification e-mail could not be sent",
          null);
    }
  }

  boolean matches(UUID userId, String code) {
    return otp.matches(userId, digest(code));
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
      LOG.warn("Email verification cleanup unavailable; the original expiry is retained");
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
