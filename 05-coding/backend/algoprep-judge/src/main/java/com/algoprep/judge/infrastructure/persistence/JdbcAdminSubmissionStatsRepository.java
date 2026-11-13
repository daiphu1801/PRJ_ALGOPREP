package com.algoprep.judge.infrastructure.persistence;

import com.algoprep.judge.domain.model.AdminSubmissionStats.DayCount;
import com.algoprep.judge.domain.model.AdminSubmissionStats.LanguageDay;
import com.algoprep.judge.domain.model.AdminSubmissionStats.MonthCount;
import com.algoprep.judge.domain.model.AdminSubmissionStats.ProblemCount;
import com.algoprep.judge.domain.model.AdminSubmissionStats.Totals;
import com.algoprep.judge.domain.ports.out.AdminSubmissionStatsRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcAdminSubmissionStatsRepository implements AdminSubmissionStatsRepository {
  private static final String DAY = "(submitted_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date";
  private static final String WINDOW =
      " FROM judge.submissions WHERE submitted_at>=:from AND submitted_at<=:asOf";
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcAdminSubmissionStatsRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Instant now() {
    return jdbc.getObject()
        .sql("SELECT clock_timestamp()")
        .query(OffsetDateTime.class)
        .single()
        .toInstant();
  }

  @Override
  public Totals totals(Instant asOf) {
    return query(
            """
        SELECT count(*) AS total,
          count(*) FILTER(WHERE submitted_at>=:current) AS current_count,
          count(*) FILTER(WHERE submitted_at>=:previous AND submitted_at<:current) AS previous_count
        FROM judge.submissions WHERE submitted_at<=:asOf
        """,
            asOf)
        .param("current", asOf.minus(Duration.ofDays(30)).atOffset(ZoneOffset.UTC))
        .param("previous", asOf.minus(Duration.ofDays(60)).atOffset(ZoneOffset.UTC))
        .query(
            (rs, n) ->
                new Totals(
                    rs.getLong("total"), rs.getLong("current_count"), rs.getLong("previous_count")))
        .single();
  }

  @Override
  public List<DayCount> daily(Instant from, Instant asOf) {
    return query(
            "SELECT " + DAY + " AS day,count(*) AS n" + WINDOW + " GROUP BY day ORDER BY day", asOf)
        .param("from", from.atOffset(ZoneOffset.UTC))
        .query((rs, n) -> new DayCount(rs.getObject("day", LocalDate.class), rs.getLong("n")))
        .list();
  }

  @Override
  public List<LanguageDay> languages(Instant from, Instant asOf) {
    return query(
            "SELECT "
                + DAY
                + " AS day,language,count(*) AS n"
                + WINDOW
                + " GROUP BY day,language ORDER BY day,language",
            asOf)
        .param("from", from.atOffset(ZoneOffset.UTC))
        .query(
            (rs, n) ->
                new LanguageDay(
                    rs.getObject("day", LocalDate.class),
                    rs.getString("language"),
                    rs.getLong("n")))
        .list();
  }

  @Override
  public Map<String, Long> verdicts(Instant asOf) {
    var rows =
        query(
                """
        SELECT status,count(*) AS n FROM judge.submissions WHERE submitted_at<=:asOf
          AND status IN ('ACCEPTED','WRONG_ANSWER','TIME_LIMIT_EXCEEDED','RUNTIME_ERROR','COMPILE_ERROR')
        GROUP BY status
        """,
                asOf)
            .query((rs, n) -> Map.entry(rs.getString("status"), rs.getLong("n")))
            .list();
    Map<String, Long> result = new LinkedHashMap<>();
    rows.forEach(entry -> result.put(entry.getKey(), entry.getValue()));
    return Map.copyOf(result);
  }

  @Override
  public List<MonthCount> months(Instant from, Instant asOf) {
    return query(
            "SELECT date_trunc('month',submitted_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date AS month,count(*) AS n"
                + WINDOW
                + " GROUP BY month ORDER BY month",
            asOf)
        .param("from", from.atOffset(ZoneOffset.UTC))
        .query(
            (rs, n) ->
                new MonthCount(
                    YearMonth.from(rs.getObject("month", LocalDate.class)), rs.getLong("n")))
        .list();
  }

  @Override
  public List<ProblemCount> problems(Set<UUID> eligibleIds, Instant asOf) {
    if (eligibleIds.isEmpty()) {
      return List.of();
    }
    return query(
            """
        SELECT problem_id,count(*) AS n,count(*) FILTER(WHERE status='ACCEPTED') AS ac
        FROM judge.submissions WHERE submitted_at<=:asOf AND problem_id IN (:ids)
        GROUP BY problem_id
        """,
            asOf)
        .param("ids", eligibleIds)
        .query(
            (rs, n) ->
                new ProblemCount(
                    rs.getObject("problem_id", UUID.class), rs.getLong("n"), rs.getLong("ac")))
        .list();
  }

  private JdbcClient.StatementSpec query(String sql, Instant asOf) {
    return jdbc.getObject().sql(sql).param("asOf", asOf.atOffset(ZoneOffset.UTC));
  }
}
