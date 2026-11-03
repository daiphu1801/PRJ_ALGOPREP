package com.algoprep.identity.infrastructure.persistence;

import com.algoprep.identity.domain.model.RegistrationAccount;
import com.algoprep.identity.domain.model.UserAccount;
import com.algoprep.identity.domain.ports.out.RegistrationRepository;
import com.algoprep.identity.domain.ports.out.UserAccountRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Uses the existing lower(email) unique index and row locks; no migration needed. */
@Repository
class JdbcRegistrationRepository implements RegistrationRepository {
  private final ObjectProvider<JdbcClient> jdbc;
  private final UserAccountRepository accounts;

  JdbcRegistrationRepository(ObjectProvider<JdbcClient> jdbc, UserAccountRepository accounts) {
    this.jdbc = jdbc;
    this.accounts = accounts;
  }

  @Override
  public boolean create(RegistrationAccount account) {
    int inserted =
        jdbc.getObject()
            .sql(
                """
        INSERT INTO identity.users
          (id, email, password_hash, display_name, role_id, status, must_change_password)
        SELECT :id, :email, :hash, :name, id, 'PENDING_VERIFICATION', false
        FROM identity.roles WHERE code = 'STUDENT' AND is_system = true
        ON CONFLICT (lower(email)) DO NOTHING
        """)
            .param("id", account.id())
            .param("email", account.email())
            .param("hash", account.passwordHash())
            .param("name", account.displayName())
            .update();
    return inserted == 1;
  }

  @Override
  public Optional<UserAccount> lockByEmail(String email) {
    return jdbc.getObject()
        .sql("SELECT id FROM identity.users WHERE lower(email) = lower(:email) FOR UPDATE")
        .param("email", email)
        .query(UUID.class)
        .optional()
        .flatMap(accounts::findById);
  }

  @Override
  public void activate(UUID userId) {
    jdbc.getObject()
        .sql(
            """
        UPDATE identity.users SET status = 'ACTIVE', updated_at = now()
        WHERE id = :id AND status = 'PENDING_VERIFICATION' AND must_change_password = false
        """)
        .param("id", userId)
        .update();
  }
}
