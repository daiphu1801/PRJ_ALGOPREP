package com.algoprep.identity.infrastructure.persistence;

import com.algoprep.identity.domain.model.AdminUserItem;
import com.algoprep.identity.domain.model.AdminUserPage;
import com.algoprep.identity.domain.ports.out.AdminUserRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Timestamps come from the database clock; the e-mail unique index decides duplicates. */
@Repository
class JdbcAdminUserRepository implements AdminUserRepository {
  private static final String SELECT =
      """
      SELECT u.id, u.display_name, u.email, r.id AS role_id, r.name AS role_name, r.base_category,
        u.status, u.last_active_at, u.password_hash IS NOT NULL AS has_password,
        u.anonymized_at IS NOT NULL AS anonymized,
        (SELECT count(*) FROM identity.user_problem_best_score b
          WHERE b.user_id = u.id AND b.best_verdict = 'ACCEPTED') AS solved_count,
        COALESCE((SELECT s.total_submissions FROM identity.user_submission_stats s
          WHERE s.user_id = u.id), 0) AS total_submissions
      FROM identity.users u JOIN identity.roles r ON r.id = u.role_id
      """;

  private static final RowMapper<AdminUserItem> ITEM =
      (rs, n) ->
          new AdminUserItem(
              rs.getObject("id", UUID.class),
              rs.getString("display_name"),
              rs.getString("email"),
              rs.getObject("role_id", UUID.class),
              rs.getString("role_name"),
              rs.getString("base_category"),
              rs.getString("status"),
              lastActive(rs.getObject("last_active_at", OffsetDateTime.class)),
              rs.getBoolean("has_password"),
              rs.getBoolean("anonymized"),
              rs.getLong("solved_count"),
              rs.getLong("total_submissions"));

  private final ObjectProvider<JdbcClient> jdbc;

  private static Instant lastActive(OffsetDateTime value) {
    return value == null ? null : value.toInstant();
  }

  JdbcAdminUserRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public AdminUserPage search(
      String status, String baseCategory, String keyword, int page, int pageSize) {
    JdbcClient client = jdbc.getObject();
    String pattern = keyword == null ? null : like(keyword);
    String where =
        """
        WHERE (CAST(:status AS text) IS NULL OR u.status = CAST(:status AS text))
          AND (CAST(:category AS text) IS NULL OR r.base_category = CAST(:category AS text))
          AND (CAST(:pattern AS text) IS NULL
            OR lower(u.display_name) LIKE CAST(:pattern AS text) ESCAPE '\\'
            OR lower(u.email) LIKE CAST(:pattern AS text) ESCAPE '\\')
        """;
    long total =
        client
            .sql(
                "SELECT count(*) FROM identity.users u JOIN identity.roles r ON r.id = u.role_id "
                    + where)
            .param("status", status)
            .param("category", baseCategory)
            .param("pattern", pattern)
            .query(Long.class)
            .single();
    List<AdminUserItem> items =
        client
            .sql(
                SELECT
                    + where
                    + " ORDER BY u.created_at DESC, u.id DESC LIMIT :limit OFFSET :offset")
            .param("status", status)
            .param("category", baseCategory)
            .param("pattern", pattern)
            .param("limit", pageSize)
            .param("offset", (long) (page - 1) * pageSize)
            .query(ITEM)
            .list();
    return new AdminUserPage(items, page, pageSize, total);
  }

  /** Lower-cases and escapes the LIKE wildcards so the keyword is matched literally. */
  private static String like(String keyword) {
    String escaped =
        keyword
            .toLowerCase(Locale.ROOT)
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    return "%" + escaped + "%";
  }

  @Override
  public Optional<AdminUserItem> find(UUID id) {
    return jdbc.getObject()
        .sql(SELECT + " WHERE u.id = :id")
        .param("id", id)
        .query(ITEM)
        .optional();
  }

  @Override
  public Optional<AdminUserItem> lock(UUID id) {
    return jdbc.getObject()
        .sql(SELECT + " WHERE u.id = :id FOR UPDATE OF u")
        .param("id", id)
        .query(ITEM)
        .optional();
  }

  @Override
  public long countPendingVerification() {
    return jdbc.getObject()
        .sql("SELECT count(*) FROM identity.users WHERE status = 'PENDING_VERIFICATION'")
        .query(Long.class)
        .single();
  }

  @Override
  public Map<String, Long> groupCounts() {
    Map<String, Long> counts = new LinkedHashMap<>();
    counts.put("STUDENT", 0L);
    counts.put("INSTRUCTOR", 0L);
    counts.put("ADMIN", 0L);
    counts.put("DEACTIVATED", 0L);
    jdbc.getObject()
        .sql(
            """
        SELECT CASE WHEN u.status = 'DEACTIVATED' THEN 'DEACTIVATED' ELSE r.base_category END AS grp,
          count(*) AS n
        FROM identity.users u JOIN identity.roles r ON r.id = u.role_id GROUP BY 1
        """)
        .query((rs, i) -> counts.put(rs.getString("grp"), rs.getLong("n")))
        .list();
    return counts;
  }

  @Override
  public boolean insertCreatedByAdmin(
      UUID id, String email, String passwordHash, String displayName, UUID roleId) {
    return jdbc.getObject()
            .sql(
                """
        INSERT INTO identity.users
          (id, email, password_hash, display_name, role_id, status, must_change_password)
        VALUES (:id, :email, :hash, :name, :role, 'PENDING_VERIFICATION', true)
        ON CONFLICT (lower(email)) DO NOTHING
        """)
            .param("id", id)
            .param("email", email)
            .param("hash", passwordHash)
            .param("name", displayName)
            .param("role", roleId)
            .update()
        == 1;
  }

  @Override
  public void changeRole(UUID userId, UUID roleId) {
    jdbc.getObject()
        .sql(
            "UPDATE identity.users SET role_id = :role, updated_at = clock_timestamp()"
                + " WHERE id = :id")
        .param("role", roleId)
        .param("id", userId)
        .update();
  }

  @Override
  public void deactivateByAdmin(UUID userId) {
    jdbc.getObject()
        .sql(
            """
        UPDATE identity.users SET status = 'DEACTIVATED', deactivation_source = 'ADMIN',
          deactivated_at = clock_timestamp(), updated_at = clock_timestamp()
        WHERE id = :id
        """)
        .param("id", userId)
        .update();
  }

  @Override
  public void reactivate(UUID userId) {
    jdbc.getObject()
        .sql(
            """
        UPDATE identity.users SET status = 'ACTIVE', deactivation_source = NULL,
          deactivated_at = NULL, updated_at = clock_timestamp()
        WHERE id = :id
        """)
        .param("id", userId)
        .update();
  }

  @Override
  public void setTemporaryPassword(UUID userId, String passwordHash) {
    jdbc.getObject()
        .sql(
            """
        UPDATE identity.users SET password_hash = :hash, must_change_password = true,
          password_changed_at = clock_timestamp(), updated_at = clock_timestamp()
        WHERE id = :id
        """)
        .param("hash", passwordHash)
        .param("id", userId)
        .update();
  }

  @Override
  public void revokeAllSessions(UUID userId) {
    jdbc.getObject()
        .sql(
            "UPDATE identity.refresh_tokens SET revoked_at = clock_timestamp()"
                + " WHERE user_id = :id AND revoked_at IS NULL")
        .param("id", userId)
        .update();
  }
}
