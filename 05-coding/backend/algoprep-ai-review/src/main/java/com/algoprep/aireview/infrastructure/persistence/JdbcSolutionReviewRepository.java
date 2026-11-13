package com.algoprep.aireview.infrastructure.persistence;

import com.algoprep.aireview.domain.model.RecentSolutionReview;
import com.algoprep.aireview.domain.model.ReviewCacheEntry;
import com.algoprep.aireview.domain.model.StoredSolutionReview;
import com.algoprep.aireview.domain.ports.out.SolutionReviewRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcSolutionReviewRepository implements SolutionReviewRepository {
  private static final String COLUMNS =
      "id, submission_id, user_id, problem_id, language, source_hash, prompt_template_id,"
          + " result_json::text AS result_json, readability_score, served_from_cache, created_at";

  private final ObjectProvider<JdbcClient> jdbc;

  JdbcSolutionReviewRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<StoredSolutionReview> findBySubmission(UUID submissionId) {
    return jdbc.getObject()
        .sql("SELECT " + COLUMNS + " FROM ai.solution_reviews WHERE submission_id = :id")
        .param("id", submissionId)
        .query(JdbcSolutionReviewRepository::stored)
        .optional();
  }

  @Override
  public Optional<ReviewCacheEntry> findCache(
      UUID problemId, String language, String sourceHash, UUID promptTemplateId) {
    return jdbc.getObject()
        .sql(
            "SELECT c.id AS cache_id, r.id AS review_id, r.result_json::text AS result_json,"
                + " r.readability_score FROM ai.ai_cache_entries c"
                + " JOIN ai.solution_reviews r ON r.id = c.solution_review_id"
                + " WHERE c.problem_id = :problem AND c.language = :language"
                + " AND c.source_hash = :hash AND c.prompt_template_id = :template")
        .param("problem", problemId)
        .param("language", language)
        .param("hash", sourceHash)
        .param("template", promptTemplateId)
        .query(
            (rs, row) ->
                new ReviewCacheEntry(
                    rs.getObject("cache_id", UUID.class),
                    rs.getObject("review_id", UUID.class),
                    rs.getString("result_json"),
                    rs.getInt("readability_score")))
        .optional();
  }

  @Override
  public boolean insertReview(StoredSolutionReview r) {
    return jdbc.getObject()
            .sql(
                "INSERT INTO ai.solution_reviews (id, submission_id, user_id, problem_id, language,"
                    + " source_hash, prompt_template_id, result_json, readability_score,"
                    + " served_from_cache, created_at) VALUES (:id, :submission, :user, :problem,"
                    + " :language, :hash, :template, CAST(:json AS jsonb), :readability, :cached,"
                    + " :createdAt) ON CONFLICT (submission_id) DO NOTHING")
            .param("id", r.id())
            .param("submission", r.submissionId())
            .param("user", r.userId())
            .param("problem", r.problemId())
            .param("language", r.language())
            .param("hash", r.sourceHash())
            .param("template", r.promptTemplateId())
            .param("json", r.resultJson())
            .param("readability", r.readabilityScore())
            .param("cached", r.servedFromCache())
            .param("createdAt", OffsetDateTime.ofInstant(r.createdAt(), ZoneOffset.UTC))
            .update()
        == 1;
  }

  @Override
  public boolean insertCache(UUID cacheId, StoredSolutionReview original) {
    return jdbc.getObject()
            .sql(
                "INSERT INTO ai.ai_cache_entries (id, problem_id, language, source_hash,"
                    + " prompt_template_id, solution_review_id) VALUES (:id, :problem, :language,"
                    + " :hash, :template, :review) ON CONFLICT ON CONSTRAINT"
                    + " ai_cache_entries_key_uk DO NOTHING")
            .param("id", cacheId)
            .param("problem", original.problemId())
            .param("language", original.language())
            .param("hash", original.sourceHash())
            .param("template", original.promptTemplateId())
            .param("review", original.id())
            .update()
        == 1;
  }

  @Override
  public void incrementHit(UUID cacheId) {
    jdbc.getObject()
        .sql("UPDATE ai.ai_cache_entries SET hit_count = hit_count + 1 WHERE id = :id")
        .param("id", cacheId)
        .update();
  }

  @Override
  public List<UUID> distinctProblemIds(UUID userId) {
    return jdbc.getObject()
        .sql("SELECT DISTINCT problem_id FROM ai.solution_reviews WHERE user_id = :user")
        .param("user", userId)
        .query(UUID.class)
        .list();
  }

  @Override
  public Optional<RecentSolutionReview> latestForProblem(UUID userId, UUID problemId) {
    return jdbc.getObject()
        .sql(
            "SELECT id, submission_id, problem_id, result_json::text AS result_json, created_at"
                + " FROM ai.solution_reviews WHERE user_id = :user AND problem_id = :problem"
                + " ORDER BY created_at DESC, id LIMIT 1")
        .param("user", userId)
        .param("problem", problemId)
        .query(JdbcSolutionReviewRepository::recent)
        .optional();
  }

  @Override
  public List<RecentSolutionReview> recent(UUID userId, int limit) {
    return jdbc.getObject()
        .sql(
            "SELECT id, submission_id, problem_id, result_json::text AS result_json, created_at"
                + " FROM ai.solution_reviews WHERE user_id = :user"
                + " ORDER BY created_at DESC, id LIMIT :limit")
        .param("user", userId)
        .param("limit", limit)
        .query(JdbcSolutionReviewRepository::recent)
        .list();
  }

  private static StoredSolutionReview stored(ResultSet rs, int row) throws SQLException {
    return new StoredSolutionReview(
        rs.getObject("id", UUID.class),
        rs.getObject("submission_id", UUID.class),
        rs.getObject("user_id", UUID.class),
        rs.getObject("problem_id", UUID.class),
        rs.getString("language"),
        rs.getString("source_hash"),
        rs.getObject("prompt_template_id", UUID.class),
        rs.getString("result_json"),
        rs.getInt("readability_score"),
        rs.getBoolean("served_from_cache"),
        rs.getObject("created_at", OffsetDateTime.class).toInstant());
  }

  private static RecentSolutionReview recent(ResultSet rs, int row) throws SQLException {
    return new RecentSolutionReview(
        rs.getObject("id", UUID.class),
        rs.getObject("submission_id", UUID.class),
        rs.getObject("problem_id", UUID.class),
        rs.getString("result_json"),
        rs.getObject("created_at", OffsetDateTime.class).toInstant());
  }
}
