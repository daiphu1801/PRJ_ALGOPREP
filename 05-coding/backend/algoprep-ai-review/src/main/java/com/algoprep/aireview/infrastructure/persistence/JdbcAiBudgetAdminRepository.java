package com.algoprep.aireview.infrastructure.persistence;

import com.algoprep.aireview.domain.model.BudgetConfig;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.LimitsConfig;
import com.algoprep.aireview.domain.model.LimitsPatch;
import com.algoprep.aireview.domain.ports.out.AiBudgetAdminRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcAiBudgetAdminRepository implements AiBudgetAdminRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcAiBudgetAdminRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  private static LimitsConfig limits(ResultSet rs, int row) throws SQLException {
    return new LimitsConfig(
        rs.getInt("review_per_hour"),
        rs.getInt("interview_per_hour"),
        rs.getInt("cooldown_seconds"),
        rs.getInt("warn_percent"));
  }

  @Override
  public LimitsConfig readLimits() {
    return jdbc.getObject()
        .sql(
            "SELECT review_per_hour, interview_per_hour, cooldown_seconds, warn_percent"
                + " FROM ai.ai_token_budget_configs WHERE id = 1")
        .query(JdbcAiBudgetAdminRepository::limits)
        .single();
  }

  @Override
  public Map<FeatureCode, Long> tokensByFeatureSince(Instant since) {
    Map<FeatureCode, Long> result = new EnumMap<>(FeatureCode.class);
    jdbc.getObject()
        .sql(
            "SELECT feature_code, SUM(tokens_prompt::bigint + tokens_completion) AS tokens"
                + " FROM ai.token_usage WHERE created_at >= :since GROUP BY feature_code")
        .param("since", OffsetDateTime.ofInstant(since, ZoneOffset.UTC))
        .query(
            rs -> {
              result.put(FeatureCode.valueOf(rs.getString("feature_code")), rs.getLong("tokens"));
            });
    return result;
  }

  @Override
  public BudgetConfig readConfigForUpdate() {
    return jdbc.getObject()
        .sql(
            "SELECT limit_tokens, period_start, locked_at, review_per_hour, interview_per_hour,"
                + " cooldown_seconds FROM ai.ai_token_budget_configs WHERE id = 1 FOR UPDATE")
        .query(
            (rs, row) -> {
              OffsetDateTime locked = rs.getObject("locked_at", OffsetDateTime.class);
              return new BudgetConfig(
                  rs.getLong("limit_tokens"),
                  rs.getObject("period_start", LocalDate.class),
                  locked == null ? null : locked.toInstant(),
                  rs.getInt("review_per_hour"),
                  rs.getInt("interview_per_hour"),
                  rs.getInt("cooldown_seconds"));
            })
        .single();
  }

  @Override
  public long updateLimit(long newLimit, UUID actor) {
    return jdbc.getObject()
        .sql(
            "WITH old AS (SELECT limit_tokens FROM ai.ai_token_budget_configs WHERE id = 1"
                + " FOR UPDATE)"
                + " UPDATE ai.ai_token_budget_configs c SET limit_tokens = :limit,"
                + " updated_by = :actor, updated_at = now() FROM old WHERE c.id = 1"
                + " RETURNING old.limit_tokens")
        .param("limit", newLimit)
        .param("actor", actor)
        .query(Long.class)
        .single();
  }

  @Override
  public void clearLock(UUID actor) {
    jdbc.getObject()
        .sql(
            "UPDATE ai.ai_token_budget_configs SET locked_at = NULL, updated_by = :actor,"
                + " updated_at = now() WHERE id = 1")
        .param("actor", actor)
        .update();
  }

  @Override
  public LimitsConfig updateLimits(LimitsPatch patch, UUID actor) {
    return jdbc.getObject()
        .sql(
            "UPDATE ai.ai_token_budget_configs SET"
                + " review_per_hour = COALESCE(:review, review_per_hour),"
                + " interview_per_hour = COALESCE(:interview, interview_per_hour),"
                + " cooldown_seconds = COALESCE(:cooldown, cooldown_seconds),"
                + " warn_percent = COALESCE(:warn, warn_percent),"
                + " updated_by = :actor, updated_at = now() WHERE id = 1"
                + " RETURNING review_per_hour, interview_per_hour, cooldown_seconds, warn_percent")
        .param("review", patch.reviewPerHour(), Types.INTEGER)
        .param("interview", patch.interviewPerHour(), Types.INTEGER)
        .param("cooldown", patch.cooldownSeconds(), Types.INTEGER)
        .param("warn", patch.warnPercent(), Types.INTEGER)
        .param("actor", actor)
        .query(JdbcAiBudgetAdminRepository::limits)
        .single();
  }

  @Override
  public boolean tryRolloverLock() {
    return Boolean.TRUE.equals(
        jdbc.getObject()
            .sql("SELECT pg_try_advisory_xact_lock(hashtextextended('ai:budget:rollover', 0))")
            .query(Boolean.class)
            .single());
  }

  @Override
  public int rollover(LocalDate monthStart) {
    return jdbc.getObject()
        .sql(
            "UPDATE ai.ai_token_budget_configs SET period_start = :month, locked_at = NULL,"
                + " updated_by = NULL, updated_at = now() WHERE id = 1 AND period_start < :month")
        .param("month", monthStart)
        .update();
  }
}
