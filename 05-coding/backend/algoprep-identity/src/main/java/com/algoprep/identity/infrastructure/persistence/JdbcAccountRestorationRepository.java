package com.algoprep.identity.infrastructure.persistence;

import com.algoprep.identity.domain.model.RestorableAccount;
import com.algoprep.identity.domain.model.RestorationGrant;
import com.algoprep.identity.domain.model.UserStatus;
import com.algoprep.identity.domain.ports.out.AccountRestorationRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Uses existing users lifecycle columns; no schema change. */
@Repository
class JdbcAccountRestorationRepository implements AccountRestorationRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcAccountRestorationRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<RestorableAccount> lockById(UUID userId) {
    return jdbc.getObject()
        .sql(
            """
        SELECT id, status, deactivation_source, deactivated_at, anonymized_at
        FROM identity.users WHERE id = :id FOR UPDATE
        """)
        .param("id", userId)
        .query(
            (rs, n) ->
                new RestorableAccount(
                    rs.getObject("id", UUID.class),
                    UserStatus.valueOf(rs.getString("status")),
                    rs.getString("deactivation_source"),
                    instant(rs.getTimestamp("deactivated_at")),
                    instant(rs.getTimestamp("anonymized_at")),
                    jdbc.getObject()
                        .sql("SELECT clock_timestamp()")
                        .query(Timestamp.class)
                        .single()
                        .toInstant()))
        .optional();
  }

  @Override
  public boolean restore(RestorationGrant grant) {
    return jdbc.getObject()
            .sql(
                """
        UPDATE identity.users SET status = 'ACTIVE', deactivation_source = NULL,
          deactivated_at = NULL, updated_at = clock_timestamp()
        WHERE id = :id AND status = 'DEACTIVATED' AND deactivation_source = 'SELF'
          AND anonymized_at IS NULL AND deactivated_at = :deactivated
          AND deactivated_at <= clock_timestamp()
          AND deactivated_at + interval '30 days' > clock_timestamp()
          AND CAST(:expires AS timestamptz) > clock_timestamp()
        """)
            .param("id", grant.userId())
            .param("deactivated", Timestamp.from(grant.deactivatedAt()))
            .param("expires", Timestamp.from(grant.expiresAt()))
            .update()
        == 1;
  }

  private static Instant instant(Timestamp value) {
    return value == null ? null : value.toInstant();
  }
}
