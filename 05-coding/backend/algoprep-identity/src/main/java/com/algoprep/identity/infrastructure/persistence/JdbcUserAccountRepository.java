package com.algoprep.identity.infrastructure.persistence;

import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.identity.domain.model.UserAccount;
import com.algoprep.identity.domain.model.UserStatus;
import com.algoprep.identity.domain.ports.out.UserAccountRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter for {@link UserAccountRepository}.
 *
 * <p>{@code JdbcClient} is fetched lazily so the application context still loads in the {@code
 * test} profile, which runs without a DataSource by project policy.
 */
@Repository
class JdbcUserAccountRepository implements UserAccountRepository {

  private static final String SELECT =
      """
      SELECT u.id, u.password_hash, u.display_name, u.status, u.must_change_password,
             r.code AS role_code, r.base_category
      FROM identity.users u JOIN identity.roles r ON r.id = u.role_id
      """;

  private final ObjectProvider<JdbcClient> jdbcProvider;

  JdbcUserAccountRepository(ObjectProvider<JdbcClient> jdbcProvider) {
    this.jdbcProvider = jdbcProvider;
  }

  @Override
  public Optional<UserAccount> findByEmail(String email) {
    return jdbcProvider
        .getObject()
        .sql(SELECT + " WHERE lower(u.email) = lower(:email)")
        .param("email", email)
        .query(JdbcUserAccountRepository::map)
        .optional();
  }

  @Override
  public Optional<UserAccount> findById(UUID id) {
    return jdbcProvider
        .getObject()
        .sql(SELECT + " WHERE u.id = :id")
        .param("id", id)
        .query(JdbcUserAccountRepository::map)
        .optional();
  }

  @Override
  public void markActive(UUID id) {
    jdbcProvider
        .getObject()
        .sql(
            "UPDATE identity.users SET status = 'ACTIVE', updated_at = now()"
                + " WHERE id = :id AND status = 'PENDING_VERIFICATION'")
        .param("id", id)
        .update();
  }

  @Override
  public void touchLastActive(UUID id, Instant now) {
    jdbcProvider
        .getObject()
        .sql(
            "UPDATE identity.users SET last_active_at = CAST(:now AS timestamptz) WHERE id = :id"
                + " AND (last_active_at IS NULL OR last_active_at < CAST(:now AS timestamptz) - interval '1 minute')")
        .param("now", Timestamp.from(now))
        .param("id", id)
        .update();
  }

  private static UserAccount map(ResultSet rs, int rowNum) throws SQLException {
    return new UserAccount(
        rs.getObject("id", UUID.class),
        rs.getString("password_hash"),
        rs.getString("display_name"),
        UserStatus.valueOf(rs.getString("status")),
        rs.getBoolean("must_change_password"),
        rs.getString("role_code"),
        BaseCategory.valueOf(rs.getString("base_category")));
  }
}
