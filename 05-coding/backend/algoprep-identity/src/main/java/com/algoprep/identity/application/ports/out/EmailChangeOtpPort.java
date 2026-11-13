package com.algoprep.identity.application.ports.out;

import com.algoprep.identity.application.dto.EmailChangeIssueResult;
import java.util.Optional;
import java.util.UUID;

/**
 * Pending e-mail change of one user: the new address and the OTP digest, TTL 600s. Dependency
 * errors propagate; confirmation never fails open.
 */
public interface EmailChangeOtpPort {

  /** Atomically checks cooldown and budget, then stores the digest for {@code newEmail}. */
  EmailChangeIssueResult issue(UUID userId, String newEmail, String digest);

  /**
   * Checks without consuming; a wrong digest counts one failure atomically, preserving the expiry,
   * and five failures lock the code.
   *
   * @return the pending new e-mail when the digest matches
   */
  Optional<String> match(UUID userId, String digest);

  /** Deletes only the matching issuance, so delayed cleanup cannot delete a newer code. */
  void clear(UUID userId, String digest);
}
