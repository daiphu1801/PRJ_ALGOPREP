package com.algoprep.aireview.infrastructure.persistence;

import com.algoprep.aireview.domain.model.BudgetConfig;
import com.algoprep.aireview.domain.ports.out.AiBudgetRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcAiBudgetRepository implements AiBudgetRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcAiBudgetRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public BudgetConfig readConfig() {
    return jdbc.getObject()
        .sql(
            "SELECT limit_tokens, period_start, locked_at, review_per_hour, interview_per_hour,"
                + " cooldown_seconds FROM ai.ai_token_budget_configs WHERE id = 1")
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
  public long usedTokensSince(Instant since) {
    return jdbc.getObject()
        .sql(
            "SELECT COALESCE(SUM(tokens_prompt::bigint + tokens_completion), 0)"
                + " FROM ai.token_usage WHERE created_at >= :since")
        .param("since", OffsetDateTime.ofInstant(since, java.time.ZoneOffset.UTC))
        .query(Long.class)
        .single();
  }

  @Override
  public boolean lockIfUnlocked() {
    return jdbc.getObject()
            .sql(
                "UPDATE ai.ai_token_budget_configs SET locked_at = now()"
                    + " WHERE id = 1 AND locked_at IS NULL")
            .update()
        == 1;
  }
}
