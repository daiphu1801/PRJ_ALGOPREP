package com.algoprep.identity.infrastructure.persistence;

import com.algoprep.identity.domain.model.ActionMatch;
import com.algoprep.identity.domain.model.ActiveAdmin;
import com.algoprep.identity.domain.model.AuditActionCatalog;
import com.algoprep.identity.domain.model.AuditLogCursor;
import com.algoprep.identity.domain.model.AuditLogFilter;
import com.algoprep.identity.domain.model.AuditLogRow;
import com.algoprep.identity.domain.ports.out.AuditLogReadRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Reads {@code identity.system_audit_logs} joined with its actor. Filters are built from fixed SQL
 * fragments and every value is a bound parameter. Time windows use {@code clock_timestamp()}, never
 * the application clock. Rows are returned raw; masking happens in the application layer.
 *
 * <p>ponytail: the keyword and category filters evaluate the action prefix table per row, so a
 * filtered search scans in {@code created_at} order until it has enough rows. Fine for the
 * retention window of a capstone (90 days); at millions of rows add a stored {@code category}
 * column.
 */
@Repository
class JdbcAuditLogReadRepository implements AuditLogReadRepository {
  private static final int DEFAULT_RETENTION_DAYS = 90;

  private static final String SELECT =
      "SELECT a.id, a.created_at, u.display_name, u.email, a.action_type, a.target_type,"
          + " a.target_id, a.before_json::text AS before_json, a.after_json::text AS after_json"
          + " FROM identity.system_audit_logs a JOIN identity.users u ON u.id = a.actor_user_id"
          + " WHERE TRUE";

  private static final String PREFIX_MATCH =
      "EXISTS (SELECT 1 FROM unnest(string_to_array(:%s, ',')) AS p(x)"
          + " WHERE starts_with(a.action_type, p.x))";

  private final ObjectProvider<JdbcClient> jdbc;

  JdbcAuditLogReadRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<AuditLogRow> search(AuditLogFilter filter, AuditLogCursor after, int limit) {
    StringBuilder sql = new StringBuilder(SELECT);
    Map<String, Object> params = new LinkedHashMap<>();
    if (filter.from() != null) {
      sql.append(" AND a.created_at >= :from");
      params.put("from", Timestamp.from(filter.from()));
    }
    if (filter.toExclusive() != null) {
      sql.append(" AND a.created_at < :to");
      params.put("to", Timestamp.from(filter.toExclusive()));
    }
    if (filter.category() != null) {
      sql.append(" AND ")
          .append(matches(AuditActionCatalog.ofCategory(filter.category()), "cat", params));
    }
    if (filter.keyword() != null) {
      keyword(filter.keyword(), sql, params);
    }
    if (after != null) {
      sql.append(" AND (a.created_at, a.id) < (:afterAt, :afterId)");
      params.put("afterAt", Timestamp.from(after.createdAt()));
      params.put("afterId", after.id());
    }
    sql.append(" ORDER BY a.created_at DESC, a.id DESC LIMIT :limit");
    params.put("limit", limit);
    return jdbc.getObject().sql(sql.toString()).params(params).query(this::row).list();
  }

  @Override
  public List<AuditLogRow> newer(AuditLogCursor after, AuditLogCursor upTo, int limit) {
    StringBuilder sql = new StringBuilder(SELECT);
    Map<String, Object> params = new LinkedHashMap<>();
    sql.append(" AND (a.created_at, a.id) > (:afterAt, :afterId)");
    params.put("afterAt", Timestamp.from(after.createdAt()));
    params.put("afterId", after.id());
    if (upTo != null) {
      sql.append(" AND (a.created_at, a.id) <= (:upAt, :upId)");
      params.put("upAt", Timestamp.from(upTo.createdAt()));
      params.put("upId", upTo.id());
    }
    sql.append(" ORDER BY a.created_at ASC, a.id ASC LIMIT :limit");
    params.put("limit", limit);
    return jdbc.getObject().sql(sql.toString()).params(params).query(this::row).list();
  }

  @Override
  public int retentionDays() {
    Optional<String> stored =
        jdbc.getObject()
            .sql(
                "SELECT value_json::text FROM identity.system_settings"
                    + " WHERE key = 'audit_log.retention_days'")
            .query(String.class)
            .optional();
    try {
      return stored.map(v -> Integer.parseInt(v.trim())).orElse(DEFAULT_RETENTION_DAYS);
    } catch (NumberFormatException notANumber) {
      return DEFAULT_RETENTION_DAYS;
    }
  }

  @Override
  public List<ActiveAdmin> activeAdmins() {
    return jdbc.getObject()
        .sql(
            "SELECT u.id, u.display_name, r.name AS role_name, count(*) AS cnt,"
                + " max(a.created_at) AS last_at"
                + " FROM identity.system_audit_logs a"
                + " JOIN identity.users u ON u.id = a.actor_user_id"
                + " JOIN identity.roles r ON r.id = u.role_id"
                + " WHERE r.base_category = 'ADMIN'"
                + " AND a.created_at >= clock_timestamp() - interval '24 hours'"
                + " GROUP BY u.id, u.display_name, r.name"
                + " ORDER BY cnt DESC, last_at DESC, u.id ASC LIMIT 5")
        .query(
            (rs, n) ->
                new ActiveAdmin(
                    rs.getObject("id", UUID.class),
                    rs.getString("display_name"),
                    rs.getString("role_name"),
                    rs.getLong("cnt"),
                    rs.getTimestamp("last_at").toInstant()))
        .list();
  }

  @Override
  public Map<String, Long> actionCountsLastSevenDays() {
    Map<String, Long> counts = new LinkedHashMap<>();
    jdbc.getObject()
        .sql(
            "SELECT action_type, count(*) AS cnt FROM identity.system_audit_logs"
                + " WHERE created_at >= clock_timestamp() - interval '7 days'"
                + " GROUP BY action_type")
        .query(
            rs -> {
              counts.put(rs.getString("action_type"), rs.getLong("cnt"));
            });
    return counts;
  }

  @Override
  public Optional<AuditLogCursor> latest() {
    return jdbc.getObject()
        .sql(
            "SELECT id, created_at FROM identity.system_audit_logs"
                + " ORDER BY created_at DESC, id DESC LIMIT 1")
        .query(JdbcAuditLogReadRepository::cursor)
        .optional();
  }

  @Override
  public Optional<AuditLogCursor> findWithinRetention(UUID id) {
    return jdbc.getObject()
        .sql(
            "SELECT id, created_at FROM identity.system_audit_logs WHERE id = :id"
                + " AND created_at >= clock_timestamp() - make_interval(days => :days)")
        .param("id", id)
        .param("days", retentionDays())
        .query(JdbcAuditLogReadRepository::cursor)
        .optional();
  }

  /** Keyword: actor e-mail, event code (id prefix, optional {@code evt_}) or service name. */
  private static void keyword(String keyword, StringBuilder sql, Map<String, Object> params) {
    String code = keyword.startsWith("evt_") ? keyword.substring(4) : keyword;
    sql.append(" AND (position(:kw IN lower(u.email)) > 0")
        .append(" OR starts_with(a.id::text, :code) OR ")
        .append(matches(AuditActionCatalog.ofServiceContaining(keyword), "svc", params))
        .append(')');
    params.put("kw", keyword);
    params.put("code", code);
  }

  /**
   * SQL predicate for an {@link ActionMatch}; binds its prefix lists as {@code key + Own/Known}.
   */
  private static String matches(ActionMatch match, String key, Map<String, Object> params) {
    params.put(key + "Own", String.join(",", match.prefixes()));
    String own = "(" + PREFIX_MATCH.formatted(key + "Own") + ")";
    if (!match.includeUnmapped()) {
      return own;
    }
    params.put(key + "Known", String.join(",", match.knownPrefixes()));
    return "(" + own + " OR NOT " + PREFIX_MATCH.formatted(key + "Known") + ")";
  }

  private AuditLogRow row(ResultSet rs, int rowNumber) throws SQLException {
    return new AuditLogRow(
        rs.getObject("id", UUID.class),
        rs.getTimestamp("created_at").toInstant(),
        rs.getString("display_name"),
        rs.getString("email"),
        rs.getString("action_type"),
        rs.getString("target_type"),
        rs.getString("target_id"),
        rs.getString("before_json"),
        rs.getString("after_json"));
  }

  private static AuditLogCursor cursor(ResultSet rs, int rowNumber) throws SQLException {
    return new AuditLogCursor(
        rs.getTimestamp("created_at").toInstant(), rs.getObject("id", UUID.class));
  }
}
