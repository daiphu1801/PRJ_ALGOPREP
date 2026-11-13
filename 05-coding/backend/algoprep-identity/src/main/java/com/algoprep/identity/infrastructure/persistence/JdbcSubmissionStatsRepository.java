package com.algoprep.identity.infrastructure.persistence;

import com.algoprep.identity.domain.model.GradedSubmission;
import com.algoprep.identity.domain.ports.out.SubmissionStatsRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcSubmissionStatsRepository implements SubmissionStatsRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcSubmissionStatsRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void keepBestScore(UUID userId, UUID problemId, BigDecimal ratio, String verdict) {
    jdbc.getObject()
        .sql(
            """
        INSERT INTO identity.user_problem_best_score(user_id, problem_id, best_ratio, best_verdict)
        VALUES (:user, :problem, :ratio, :verdict)
        ON CONFLICT (user_id, problem_id) DO UPDATE
          SET best_ratio = EXCLUDED.best_ratio, best_verdict = EXCLUDED.best_verdict,
              updated_at = now()
          WHERE EXCLUDED.best_ratio > identity.user_problem_best_score.best_ratio
        """)
        .param("user", userId)
        .param("problem", problemId)
        .param("ratio", ratio)
        .param("verdict", verdict)
        .update();
  }

  @Override
  public void addToTotals(GradedSubmission s) {
    boolean accepted = "ACCEPTED".equals(s.verdict());
    // The upsert takes the row lock, so the top-language statement below sees the new counts and
    // no other event of the same user can interleave before commit.
    jdbc.getObject()
        .sql(
            """
        INSERT INTO identity.user_submission_stats(
            user_id, total_submissions, ac_eligible_count, accepted_count,
            first_try_accepted_count, language_counts)
        VALUES (:user, 1, :eligible, :accepted, :firstTry,
                jsonb_build_object(CAST(:lang AS text), 1))
        ON CONFLICT (user_id) DO UPDATE SET
            total_submissions = identity.user_submission_stats.total_submissions + 1,
            ac_eligible_count = identity.user_submission_stats.ac_eligible_count + :eligible,
            accepted_count = identity.user_submission_stats.accepted_count + :accepted,
            first_try_accepted_count =
                identity.user_submission_stats.first_try_accepted_count + :firstTry,
            language_counts = jsonb_set(
                identity.user_submission_stats.language_counts, ARRAY[CAST(:lang AS text)],
                to_jsonb(COALESCE(
                    (identity.user_submission_stats.language_counts ->> CAST(:lang AS text))::int,
                    0) + 1)),
            updated_at = now()
        """)
        .param("user", s.userId())
        .param("eligible", "SYSTEM_ERROR".equals(s.verdict()) ? 0 : 1)
        .param("accepted", accepted ? 1 : 0)
        .param("firstTry", accepted && s.firstAttemptForProblem() ? 1 : 0)
        .param("lang", s.language())
        .update();
    // Largest counter wins; on a tie the language already on top stays (DD api F1 section 2),
    // then the smaller code decides when there is none yet.
    jdbc.getObject()
        .sql(
            """
        UPDATE identity.user_submission_stats st SET top_language = (
            SELECT e.key FROM jsonb_each_text(st.language_counts) e
            ORDER BY e.value::int DESC, (e.key = st.top_language) DESC, e.key ASC LIMIT 1)
        WHERE st.user_id = :user
        """)
        .param("user", s.userId())
        .update();
  }

  @Override
  public void addToDay(UUID userId, LocalDate day) {
    jdbc.getObject()
        .sql(
            """
        INSERT INTO identity.user_daily_submission_stats(user_id, day, submission_count)
        VALUES (:user, :day, 1)
        ON CONFLICT (user_id, day) DO UPDATE
          SET submission_count = identity.user_daily_submission_stats.submission_count + 1
        """)
        .param("user", userId)
        .param("day", day)
        .update();
  }
}
