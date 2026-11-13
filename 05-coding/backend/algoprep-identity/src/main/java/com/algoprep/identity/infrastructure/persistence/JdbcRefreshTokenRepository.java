package com.algoprep.identity.infrastructure.persistence;

import com.algoprep.identity.domain.model.StoredRefreshToken;
import com.algoprep.identity.domain.ports.out.RefreshTokenRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** JDBC adapter for {@link RefreshTokenRepository}; see the note on lazy {@code JdbcClient}. */
@Repository
class JdbcRefreshTokenRepository implements RefreshTokenRepository {

  private final ObjectProvider<JdbcClient> jdbcProvider;

  JdbcRefreshTokenRepository(ObjectProvider<JdbcClient> jdbcProvider) {
    this.jdbcProvider = jdbcProvider;
  }

  @Override
  public void insert(StoredRefreshToken token, String tokenHash, Instant issuedAt) {
    jdbcProvider
        .getObject()
        .sql(
            "INSERT INTO identity.refresh_tokens"
                + " (id, user_id, token_hash, family_id, issued_at, expires_at)"
                + " VALUES (:id, :userId, :hash, :familyId, :issuedAt, :expiresAt)")
        .param("id", token.id())
        .param("userId", token.userId())
        .param("hash", tokenHash)
        .param("familyId", token.familyId())
        .param("issuedAt", Timestamp.from(issuedAt))
        .param("expiresAt", Timestamp.from(token.expiresAt()))
        .update();
  }

  @Override
  public Optional<StoredRefreshToken> findByHashForUpdate(String tokenHash) {
    return jdbcProvider
        .getObject()
        .sql(
            "SELECT id, user_id, family_id, expires_at, revoked_at FROM identity.refresh_tokens"
                + " WHERE token_hash = :hash FOR UPDATE")
        .param("hash", tokenHash)
        .query(
            (rs, n) -> {
              Timestamp revoked = rs.getTimestamp("revoked_at");
              return new StoredRefreshToken(
                  rs.getObject("id", UUID.class),
                  rs.getObject("user_id", UUID.class),
                  rs.getObject("family_id", UUID.class),
                  rs.getTimestamp("expires_at").toInstant(),
                  revoked == null ? null : revoked.toInstant());
            })
        .optional();
  }

  @Override
  public void revoke(UUID id, Instant at) {
    jdbcProvider
        .getObject()
        .sql("UPDATE identity.refresh_tokens SET revoked_at = :at WHERE id = :id")
        .param("at", Timestamp.from(at))
        .param("id", id)
        .update();
  }

  @Override
  public void revokeFamily(UUID familyId, Instant at) {
    jdbcProvider
        .getObject()
        .sql(
            "UPDATE identity.refresh_tokens SET revoked_at = :at"
                + " WHERE family_id = :familyId AND revoked_at IS NULL")
        .param("at", Timestamp.from(at))
        .param("familyId", familyId)
        .update();
  }

  @Override
  public int deleteExpiredBatch(int limit) {
    return jdbcProvider
        .getObject()
        .sql(
            """
            DELETE FROM identity.refresh_tokens WHERE id IN (
              SELECT id FROM identity.refresh_tokens
              WHERE issued_at < now() - interval '6 months'
                AND (expires_at < now() OR revoked_at IS NOT NULL)
              ORDER BY issued_at LIMIT :limit)
            """)
        .param("limit", limit)
        .update();
  }
}
