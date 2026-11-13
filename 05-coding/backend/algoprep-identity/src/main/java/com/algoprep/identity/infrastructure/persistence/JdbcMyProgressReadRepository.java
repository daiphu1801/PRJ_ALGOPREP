package com.algoprep.identity.infrastructure.persistence;

import com.algoprep.identity.domain.model.ActivityEntry;
import com.algoprep.identity.domain.model.ProgressCounters;
import com.algoprep.identity.domain.model.SolveState;
import com.algoprep.identity.domain.ports.out.MyProgressReadRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Reads the four personal read models; the caller id is in every WHERE clause. */
@Repository
class JdbcMyProgressReadRepository implements MyProgressReadRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcMyProgressReadRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public ProgressCounters counters(UUID userId) {
    JdbcClient client = jdbc.getObject();
    int solved =
        client
            .sql(
                """
                SELECT count(*) FROM identity.user_problem_best_score
                WHERE user_id = :u AND best_verdict = 'ACCEPTED'
                """)
            .param("u", userId)
            .query(Integer.class)
            .single();
    return client
        .sql(
            """
            SELECT total_submissions, ac_eligible_count, accepted_count,
                   first_try_accepted_count, top_language
            FROM identity.user_submission_stats WHERE user_id = :u
            """)
        .param("u", userId)
        .query(
            (rs, n) ->
                new ProgressCounters(
                    solved,
                    rs.getInt("total_submissions"),
                    rs.getInt("ac_eligible_count"),
                    rs.getInt("accepted_count"),
                    rs.getInt("first_try_accepted_count"),
                    rs.getString("top_language")))
        .optional()
        .orElse(new ProgressCounters(solved, 0, 0, 0, 0, null));
  }

  @Override
  public List<ActivityEntry> recentActivity(UUID userId, Instant since, int limit) {
    return jdbc.getObject()
        .sql(
            """
            SELECT id, scope_class_id, activity_type, ref_type, ref_id, occurred_at
            FROM identity.identity_recent_activity
            WHERE actor_user_id = :u AND occurred_at >= :since
            ORDER BY occurred_at DESC, id DESC LIMIT :limit
            """)
        .param("u", userId)
        .param("since", Timestamp.from(since))
        .param("limit", limit)
        .query(
            (rs, n) ->
                new ActivityEntry(
                    rs.getObject("id", UUID.class),
                    userId,
                    rs.getObject("scope_class_id", UUID.class),
                    rs.getString("activity_type"),
                    rs.getString("ref_type"),
                    rs.getObject("ref_id", UUID.class),
                    rs.getTimestamp("occurred_at").toInstant()))
        .list();
  }

  @Override
  public Map<LocalDate, Integer> dailyCounts(UUID userId, LocalDate from, LocalDate to) {
    Map<LocalDate, Integer> counts = new HashMap<>();
    jdbc.getObject()
        .sql(
            """
            SELECT day, submission_count FROM identity.user_daily_submission_stats
            WHERE user_id = :u AND day BETWEEN :from AND :to
            """)
        .param("u", userId)
        .param("from", from)
        .param("to", to)
        .query(
            rs -> {
              counts.put(rs.getObject("day", LocalDate.class), rs.getInt("submission_count"));
            });
    return counts;
  }

  @Override
  public Map<UUID, SolveState> solveStates(UUID userId, Collection<UUID> problemIds) {
    Map<UUID, SolveState> states = new LinkedHashMap<>();
    jdbc.getObject()
        .sql(
            """
            SELECT problem_id, best_verdict FROM identity.user_problem_best_score
            WHERE user_id = :u AND problem_id IN (:ids)
            """)
        .param("u", userId)
        .param("ids", problemIds)
        .query(
            rs -> {
              states.put(
                  rs.getObject("problem_id", UUID.class),
                  "ACCEPTED".equals(rs.getString("best_verdict"))
                      ? SolveState.SOLVED
                      : SolveState.IN_PROGRESS);
            });
    return states;
  }

  @Override
  public Map<UUID, SolveState> allSolveStates(UUID userId) {
    Map<UUID, SolveState> states = new LinkedHashMap<>();
    jdbc.getObject()
        .sql(
            """
            SELECT problem_id, best_verdict FROM identity.user_problem_best_score
            WHERE user_id = :u
            """)
        .param("u", userId)
        .query(
            rs -> {
              states.put(
                  rs.getObject("problem_id", UUID.class),
                  "ACCEPTED".equals(rs.getString("best_verdict"))
                      ? SolveState.SOLVED
                      : SolveState.IN_PROGRESS);
            });
    return states;
  }
}
