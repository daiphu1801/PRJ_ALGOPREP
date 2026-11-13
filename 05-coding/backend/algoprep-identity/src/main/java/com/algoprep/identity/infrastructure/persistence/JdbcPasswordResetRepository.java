package com.algoprep.identity.infrastructure.persistence;

import com.algoprep.identity.domain.model.PasswordResetAccount;
import com.algoprep.identity.domain.ports.out.PasswordResetRepository;
import com.algoprep.identity.domain.ports.out.UserAccountRepository;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Row locks and PostgreSQL timestamps enforce one-time reset even if Redis cleanup fails. */
@Repository
class JdbcPasswordResetRepository implements PasswordResetRepository {
  private final ObjectProvider<JdbcClient> jdbc;
  private final UserAccountRepository accounts;

  JdbcPasswordResetRepository(ObjectProvider<JdbcClient> jdbc, UserAccountRepository accounts) {
    this.jdbc = jdbc;
    this.accounts = accounts;
  }

  @Override
  public Optional<PasswordResetAccount> lockByEmail(String email) {
    return jdbc.getObject()
        .sql(
            """
        SELECT id, email, password_changed_at
        FROM identity.users WHERE lower(email) = lower(:email) FOR UPDATE
        """)
        .param("email", email)
        .query(
            (rs, n) -> {
              Timestamp changed = rs.getTimestamp("password_changed_at");
              return new PasswordResetAccount(
                  accounts.findById(rs.getObject("id", UUID.class)).orElseThrow(),
                  rs.getString("email"),
                  changed == null ? null : changed.toInstant(),
                  jdbc.getObject()
                      .sql("SELECT clock_timestamp()")
                      .query(Timestamp.class)
                      .single()
                      .toInstant());
            })
        .optional();
  }

  @Override
  public void updatePasswordAndRevokeSessions(UUID userId, String passwordHash) {
    jdbc.getObject()
        .sql(
            """
        UPDATE identity.users SET password_hash = :hash, password_changed_at = clock_timestamp(),
          must_change_password = false, updated_at = clock_timestamp(),
          status = CASE WHEN status = 'PENDING_VERIFICATION' AND must_change_password = true
            THEN 'ACTIVE' ELSE status END
        WHERE id = :id
        """)
        .param("hash", passwordHash)
        .param("id", userId)
        .update();
    jdbc.getObject()
        .sql(
            """
        UPDATE identity.refresh_tokens SET revoked_at = clock_timestamp()
        WHERE user_id = :id AND revoked_at IS NULL
        """)
        .param("id", userId)
        .update();
  }
}
