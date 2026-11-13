package com.algoprep.identity.infrastructure.persistence;

import com.algoprep.identity.domain.model.UserRetentionGroup;
import com.algoprep.identity.domain.ports.out.AdminDashboardRepository;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Days and months are those of UTC+7, the zone the overview reads in (DD api F1 section 0.8.5). */
@Repository
class JdbcAdminDashboardRepository implements AdminDashboardRepository {
  private static final String ZONE = "'Asia/Ho_Chi_Minh'";

  private final ObjectProvider<JdbcClient> jdbc;

  JdbcAdminDashboardRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public long activeUsers(int fromDaysAgo, int toDaysAgo) {
    return jdbc.getObject()
        .sql(
            """
        SELECT count(DISTINCT t.user_id) FROM identity.refresh_tokens t
        JOIN identity.users u ON u.id = t.user_id
        WHERE u.status <> 'DEACTIVATED'
          AND t.issued_at >= clock_timestamp() - make_interval(days => :from)
          AND t.issued_at < clock_timestamp() - make_interval(days => :to)
        """)
        .param("from", fromDaysAgo)
        .param("to", toDaysAgo)
        .query(Long.class)
        .single();
  }

  @Override
  public long activeUsersAtDayEnd(int daysAgo) {
    return jdbc.getObject()
        .sql(
            """
        WITH edge AS (
          SELECT (date_trunc('day', clock_timestamp() AT TIME ZONE {tz})
                  + interval '1 day' - make_interval(days => :ago)) AT TIME ZONE {tz} AS day_end)
        SELECT count(DISTINCT t.user_id) FROM identity.refresh_tokens t
        JOIN identity.users u ON u.id = t.user_id, edge
        WHERE u.status <> 'DEACTIVATED'
          AND t.issued_at >= edge.day_end - interval '30 days' AND t.issued_at < edge.day_end
        """
                .replace("{tz}", ZONE))
        .param("ago", daysAgo)
        .query(Long.class)
        .single();
  }

  @Override
  public List<UserRetentionGroup> userRetention(int months) {
    return jdbc.getObject()
        .sql(
            """
        WITH m AS (
          SELECT date_trunc('month', clock_timestamp() AT TIME ZONE {tz})
                 - make_interval(months => g) AS start
          FROM generate_series(:months - 1, 0, -1) g)
        SELECT to_char(m.start, 'YYYY-MM') AS month,
          (SELECT count(*) FROM identity.users u
            WHERE u.status <> 'DEACTIVATED'
              AND u.created_at AT TIME ZONE {tz} >= m.start
              AND u.created_at AT TIME ZONE {tz} < m.start + interval '1 month') AS new_users,
          (SELECT count(DISTINCT t.user_id) FROM identity.refresh_tokens t
            JOIN identity.users u ON u.id = t.user_id
            WHERE u.status <> 'DEACTIVATED'
              AND t.issued_at AT TIME ZONE {tz} >= m.start
              AND t.issued_at AT TIME ZONE {tz} < m.start + interval '1 month'
              AND u.created_at AT TIME ZONE {tz} < m.start) AS returning_users
        FROM m ORDER BY m.start
        """
                .replace("{tz}", ZONE))
        .param("months", months)
        .query(
            (rs, row) ->
                new UserRetentionGroup(
                    rs.getString("month"), rs.getLong("new_users"), rs.getLong("returning_users")))
        .list();
  }

  @Override
  public long passwordResets(int fromHoursAgo, int toHoursAgo) {
    return jdbc.getObject()
        .sql(
            """
        SELECT count(*) FROM identity.system_audit_logs
        WHERE action_type = 'PASSWORD_RESET_REQUESTED'
          AND created_at >= clock_timestamp() - make_interval(hours => :from)
          AND created_at < clock_timestamp() - make_interval(hours => :to)
        """)
        .param("from", fromHoursAgo)
        .param("to", toHoursAgo)
        .query(Long.class)
        .single();
  }

  @Override
  public List<Long> passwordResetsPerDay(int days) {
    return jdbc.getObject()
        .sql(
            """
        WITH d AS (
          SELECT date_trunc('day', clock_timestamp() AT TIME ZONE {tz})
                 - make_interval(days => g) AS day
          FROM generate_series(:days - 1, 0, -1) g)
        SELECT (SELECT count(*) FROM identity.system_audit_logs a
                WHERE a.action_type = 'PASSWORD_RESET_REQUESTED'
                  AND a.created_at AT TIME ZONE {tz} >= d.day
                  AND a.created_at AT TIME ZONE {tz} < d.day + interval '1 day') AS total
        FROM d ORDER BY d.day
        """
                .replace("{tz}", ZONE))
        .param("days", days)
        .query(Long.class)
        .list();
  }
}
