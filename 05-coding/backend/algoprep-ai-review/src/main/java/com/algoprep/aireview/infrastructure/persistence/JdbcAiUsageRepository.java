package com.algoprep.aireview.infrastructure.persistence;

import com.algoprep.aireview.domain.model.AlertReview;
import com.algoprep.aireview.domain.model.AnomalyAlert;
import com.algoprep.aireview.domain.model.DailyFeatureTokens;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.ProblemUsage;
import com.algoprep.aireview.domain.model.UserUsage;
import com.algoprep.aireview.domain.ports.out.AiUsageRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcAiUsageRepository implements AiUsageRepository {
  /** ponytail: a fixed cap, the table of OPEN alerts stays small (one per account). */
  private static final int ALERT_LIST_CAP = 100;

  private final ObjectProvider<JdbcClient> jdbc;

  JdbcAiUsageRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  private static OffsetDateTime utc(Instant instant) {
    return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
  }

  @Override
  public List<DailyFeatureTokens> dailyTokens(Instant from, Instant to) {
    return jdbc.getObject()
        .sql(
            "SELECT (created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date AS day, feature_code,"
                + " SUM(tokens_prompt::bigint + tokens_completion) AS tokens FROM ai.token_usage"
                + " WHERE created_at >= :from AND created_at < :to GROUP BY 1, 2")
        .param("from", utc(from))
        .param("to", utc(to))
        .query(
            (rs, row) ->
                new DailyFeatureTokens(
                    rs.getObject("day", LocalDate.class),
                    FeatureCode.valueOf(rs.getString("feature_code")),
                    rs.getLong("tokens")))
        .list();
  }

  @Override
  public List<UserUsage> topUsers(Instant since, int limit) {
    return jdbc.getObject()
        .sql(
            "WITH per_model AS (SELECT u.user_id,"
                + " SUM(u.tokens_prompt::bigint + u.tokens_completion) AS tokens,"
                + " COUNT(*) AS calls, p.usd_per_million_tokens AS price"
                + " FROM ai.token_usage u LEFT JOIN ai.ai_model_prices p"
                + " ON lower(p.model_name) = lower(u.model_name)"
                + " WHERE u.created_at >= :since"
                + " GROUP BY u.user_id, lower(u.model_name), p.usd_per_million_tokens)"
                + " SELECT user_id, SUM(tokens) AS tokens, SUM(calls) AS calls,"
                + " CASE WHEN bool_or(price IS NULL) THEN NULL"
                + " ELSE round(SUM(tokens * price / 1000000), 4) END AS cost"
                + " FROM per_model GROUP BY user_id ORDER BY tokens DESC, user_id LIMIT :limit")
        .param("since", utc(since))
        .param("limit", limit)
        .query(
            (rs, row) ->
                new UserUsage(
                    rs.getObject("user_id", UUID.class),
                    rs.getLong("tokens"),
                    rs.getInt("calls"),
                    rs.getBigDecimal("cost")))
        .list();
  }

  @Override
  public List<ProblemUsage> topProblems(Instant since, int limit) {
    return jdbc.getObject()
        .sql(
            "SELECT problem_id, SUM(tokens_prompt::bigint + tokens_completion) AS tokens,"
                + " COUNT(*) AS calls FROM ai.token_usage"
                + " WHERE created_at >= :since AND problem_id IS NOT NULL"
                + " GROUP BY problem_id ORDER BY tokens DESC, problem_id LIMIT :limit")
        .param("since", utc(since))
        .param("limit", limit)
        .query(
            (rs, row) ->
                new ProblemUsage(
                    rs.getObject("problem_id", UUID.class),
                    rs.getLong("tokens"),
                    rs.getInt("calls")))
        .list();
  }

  @Override
  public List<AnomalyAlert> openAlerts() {
    return jdbc.getObject()
        .sql(
            "SELECT id, user_id, window_start, window_end, call_count, baseline_avg, ratio,"
                + " created_at FROM ai.ai_usage_anomaly_alerts WHERE status = 'OPEN'"
                + " ORDER BY created_at DESC, id LIMIT :cap")
        .param("cap", ALERT_LIST_CAP)
        .query(
            (rs, row) ->
                new AnomalyAlert(
                    rs.getObject("id", UUID.class),
                    rs.getObject("user_id", UUID.class),
                    rs.getObject("window_start", OffsetDateTime.class).toInstant(),
                    rs.getObject("window_end", OffsetDateTime.class).toInstant(),
                    rs.getInt("call_count"),
                    rs.getBigDecimal("baseline_avg"),
                    rs.getBigDecimal("ratio"),
                    rs.getObject("created_at", OffsetDateTime.class).toInstant()))
        .list();
  }

  @Override
  public Optional<AlertReview> markReviewed(UUID alertId) {
    Optional<OffsetDateTime> updated =
        jdbc.getObject()
            .sql(
                "UPDATE ai.ai_usage_anomaly_alerts SET status = 'REVIEWED', reviewed_at = now()"
                    + " WHERE id = :id AND status = 'OPEN' RETURNING reviewed_at")
            .param("id", alertId)
            .query(OffsetDateTime.class)
            .optional();
    if (updated.isPresent()) {
      return Optional.of(new AlertReview(alertId, updated.get().toInstant(), false));
    }
    return jdbc.getObject()
        .sql("SELECT reviewed_at FROM ai.ai_usage_anomaly_alerts WHERE id = :id")
        .param("id", alertId)
        .query(OffsetDateTime.class)
        .optional()
        .map(at -> new AlertReview(alertId, at.toInstant(), true));
  }

  @Override
  public boolean tryAnomalyLock() {
    return Boolean.TRUE.equals(
        jdbc.getObject()
            .sql("SELECT pg_try_advisory_xact_lock(hashtextextended('ai:usage:anomaly', 0))")
            .query(Boolean.class)
            .single());
  }

  @Override
  public Map<UUID, Integer> callCountsSince(Instant since) {
    Map<UUID, Integer> counts = new HashMap<>();
    jdbc.getObject()
        .sql(
            "SELECT user_id, COUNT(*) AS calls FROM ai.token_usage WHERE created_at >= :since"
                + " GROUP BY user_id")
        .param("since", utc(since))
        .query(
            rs -> {
              counts.put(rs.getObject("user_id", UUID.class), rs.getInt("calls"));
            });
    return counts;
  }

  @Override
  public Set<UUID> usersWithOpenAlert() {
    return new HashSet<>(
        jdbc.getObject()
            .sql("SELECT DISTINCT user_id FROM ai.ai_usage_anomaly_alerts WHERE status = 'OPEN'")
            .query(UUID.class)
            .list());
  }

  @Override
  public void insertAlert(AnomalyAlert alert) {
    jdbc.getObject()
        .sql(
            "INSERT INTO ai.ai_usage_anomaly_alerts (id, user_id, window_start, window_end,"
                + " call_count, baseline_avg, ratio, status, created_at)"
                + " VALUES (:id, :user, :start, :end, :calls, :baseline, :ratio, 'OPEN', :at)")
        .param("id", alert.id())
        .param("user", alert.userId())
        .param("start", utc(alert.windowStart()))
        .param("end", utc(alert.windowEnd()))
        .param("calls", alert.callCount())
        .param("baseline", alert.baselineAvg())
        .param("ratio", alert.ratio())
        .param("at", utc(alert.createdAt()))
        .update();
  }
}
