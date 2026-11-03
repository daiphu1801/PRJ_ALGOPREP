package com.algoprep.problembank.infrastructure.persistence;

import com.algoprep.problembank.domain.model.SubmissionProblemSummary;
import com.algoprep.problembank.domain.ports.out.SubmissionMetadataRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcSubmissionMetadataRepository implements SubmissionMetadataRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcSubmissionMetadataRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<SubmissionProblemSummary> summaries(Set<UUID> ids) {
    return jdbc.getObject()
        .sql(
            """
        SELECT p.id,p.code,p.title,l.display_name,(p.deleted OR p.status<>'PUBLISHED') AS withdrawn
        FROM problem.problems p JOIN problem.problem_levels l ON l.id=p.level_id WHERE p.id IN (:ids)
        """)
        .param("ids", ids)
        .query(
            (rs, n) ->
                new SubmissionProblemSummary(
                    rs.getObject("id", UUID.class),
                    rs.getString("code"),
                    rs.getString("title"),
                    rs.getString("code"),
                    rs.getString("display_name"),
                    rs.getBoolean("withdrawn")))
        .list();
  }

  @Override
  public List<UUID> search(String keyword, int limit) {
    // strpos treats percent, underscore and backslash as literal search characters.
    return jdbc.getObject()
        .sql(
            """
        SELECT id FROM problem.problems WHERE strpos(lower(code),lower(:q))>0
        OR strpos(lower(title),lower(:q))>0 ORDER BY code,id LIMIT :limit
        """)
        .param("q", keyword)
        .param("limit", limit)
        .query(UUID.class)
        .list();
  }

  @Override
  public Set<UUID> samples(UUID problemId, Set<UUID> testcaseIds) {
    return new HashSet<>(
        jdbc.getObject()
            .sql(
                """
        SELECT id FROM problem.testcases WHERE problem_id=:problem AND id IN (:ids)
        AND visibility='SAMPLE'
        """)
            .param("problem", problemId)
            .param("ids", testcaseIds)
            .query(UUID.class)
            .list());
  }
}
