package com.algoprep.identity.domain.ports.out;

import com.algoprep.identity.domain.model.AdminUserItem;
import com.algoprep.identity.domain.model.AdminUserPage;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Account reads and writes of the admin user management screen (#35 to #38, #50, #62, #63). */
public interface AdminUserRepository {

  /**
   * Filtered page, newest account first. Null filters mean "any"; {@code baseCategory} is the
   * layer-1 category of the role.
   */
  AdminUserPage search(String status, String baseCategory, String keyword, int page, int pageSize);

  /** One account, empty when it does not exist. */
  Optional<AdminUserItem> find(UUID id);

  /** Like {@link #find} but locks the row until the caller's transaction ends. */
  Optional<AdminUserItem> lock(UUID id);

  /** Accounts waiting for e-mail verification (#62). */
  long countPendingVerification();

  /**
   * Account counts for {@code STUDENT}, {@code INSTRUCTOR}, {@code ADMIN} (not deactivated) and
   * {@code DEACTIVATED} (every role), each account counted once (#63).
   */
  Map<String, Long> groupCounts();

  /**
   * Inserts an admin-created account: {@code PENDING_VERIFICATION}, must change the password.
   *
   * @return false when the e-mail is already taken (case-insensitive)
   */
  boolean insertCreatedByAdmin(
      UUID id, String email, String passwordHash, String displayName, UUID roleId);

  void changeRole(UUID userId, UUID roleId);

  /** Sets {@code DEACTIVATED} with source {@code ADMIN}. */
  void deactivateByAdmin(UUID userId);

  /** Sets {@code ACTIVE} and clears the deactivation source and time. */
  void reactivate(UUID userId);

  /** Stores a temporary password hash and forces a change at the next login. */
  void setTemporaryPassword(UUID userId, String passwordHash);

  /** Revokes every still-usable refresh token of the user. */
  void revokeAllSessions(UUID userId);
}
