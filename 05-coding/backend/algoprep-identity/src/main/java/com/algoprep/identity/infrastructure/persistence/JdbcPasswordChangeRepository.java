package com.algoprep.identity.infrastructure.persistence;

import com.algoprep.identity.domain.model.PasswordChangeAccount;
import com.algoprep.identity.domain.ports.out.PasswordChangeRepository;
import com.algoprep.identity.domain.ports.out.UserAccountRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Row lock on the user serialises concurrent changes of the same account. */
@Repository
class JdbcPasswordChangeRepository implements PasswordChangeRepository {
  private final ObjectProvider<JdbcClient> jdbc;
  private final UserAccountRepository accounts;

  JdbcPasswordChangeRepository(ObjectProvider<JdbcClient> jdbc, UserAccountRepository accounts) {
    this.jdbc = jdbc;
    this.accounts = accounts;
  }

  @Override
  public Optional<PasswordChangeAccount> lockById(UUID userId) {
    return jdbc.getObject()
        .sql("SELECT email FROM identity.users WHERE id = :id FOR UPDATE")
        .param("id", userId)
        .query(String.class)
        .optional()
        .flatMap(
            email -> accounts.findById(userId).map(user -> new PasswordChangeAccount(user, email)));
  }

  @Override
  public void updatePasswordAndRevokeOtherSessions(
      UUID userId, String passwordHash, UUID keepFamilyId) {
    jdbc.getObject()
        .sql(
            """
        UPDATE identity.users SET password_hash = :hash, password_changed_at = clock_timestamp(),
          must_change_password = false, updated_at = clock_timestamp()
        WHERE id = :id
        """)
        .param("hash", passwordHash)
        .param("id", userId)
        .update();
    jdbc.getObject()
        .sql(
            """
        UPDATE identity.refresh_tokens SET revoked_at = clock_timestamp()
        WHERE user_id = :id AND family_id <> :keep AND revoked_at IS NULL
        """)
        .param("id", userId)
        .param("keep", keepFamilyId)
        .update();
  }
}
