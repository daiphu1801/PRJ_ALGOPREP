package com.algoprep.identity.infrastructure.persistence;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.identity.domain.model.OAuthProvider;
import com.algoprep.identity.domain.ports.out.OAuthAccountRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Uses the existing unique indexes on lower(email) and (provider, provider_user_id). */
@Repository
class JdbcOAuthAccountRepository implements OAuthAccountRepository {
  private final ObjectProvider<JdbcClient> jdbc;
  private final IdGeneratorPort ids;

  JdbcOAuthAccountRepository(ObjectProvider<JdbcClient> jdbc, IdGeneratorPort ids) {
    this.jdbc = jdbc;
    this.ids = ids;
  }

  @Override
  public Optional<UUID> findLinkedUser(OAuthProvider provider, String providerUserId) {
    return jdbc.getObject()
        .sql(
            """
            SELECT user_id FROM identity.oauth_identities
            WHERE provider = :provider AND provider_user_id = :id
            """)
        .param("provider", provider.name())
        .param("id", providerUserId)
        .query(UUID.class)
        .optional();
  }

  @Override
  public Optional<UUID> lockUserByEmail(String email) {
    return jdbc.getObject()
        .sql("SELECT id FROM identity.users WHERE lower(email) = lower(:email) FOR UPDATE")
        .param("email", email)
        .query(UUID.class)
        .optional();
  }

  @Override
  public Optional<UUID> createStudent(String email, String displayName) {
    UUID id = ids.newId();
    int inserted =
        jdbc.getObject()
            .sql(
                """
                INSERT INTO identity.users
                  (id, email, password_hash, display_name, role_id, status, must_change_password)
                SELECT :id, :email, NULL, :name, id, 'ACTIVE', false
                FROM identity.roles WHERE code = 'STUDENT' AND is_system = true
                ON CONFLICT (lower(email)) DO NOTHING
                """)
            .param("id", id)
            .param("email", email)
            .param("name", displayName)
            .update();
    return inserted == 1 ? Optional.of(id) : Optional.empty();
  }

  @Override
  public boolean link(
      UUID userId, OAuthProvider provider, String providerUserId, String providerEmail) {
    return jdbc.getObject()
            .sql(
                """
                INSERT INTO identity.oauth_identities
                  (id, user_id, provider, provider_user_id, provider_email)
                VALUES (:id, :user, :provider, :providerUser, :email)
                ON CONFLICT (provider, provider_user_id) DO NOTHING
                """)
            .param("id", ids.newId())
            .param("user", userId)
            .param("provider", provider.name())
            .param("providerUser", providerUserId)
            .param("email", providerEmail)
            .update()
        == 1;
  }

  @Override
  public void claimPendingAccount(UUID userId) {
    jdbc.getObject()
        .sql(
            """
            UPDATE identity.users
            SET password_hash = NULL, status = 'ACTIVE', updated_at = now()
            WHERE id = :id AND status = 'PENDING_VERIFICATION' AND must_change_password = false
            """)
        .param("id", userId)
        .update();
  }
}
