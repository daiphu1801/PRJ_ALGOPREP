package com.algoprep.problembank.infrastructure.persistence;

import com.algoprep.problembank.domain.model.GradingReadSnapshot;
import com.algoprep.problembank.domain.model.GradingSpecData;
import com.algoprep.problembank.domain.model.SnapshotMetadata;
import com.algoprep.problembank.domain.model.StoredSampleTestcase;
import com.algoprep.problembank.domain.model.TestcaseDescriptor;
import com.algoprep.problembank.domain.ports.out.GradingReadRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcGradingReadRepository implements GradingReadRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcGradingReadRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<GradingReadSnapshot> findGradingSpec(UUID problemId) {
    var rows =
        jdbc.getObject()
            .sql(
                """
        SELECT p.id, p.status, p.deleted, p.function_wrapper_supported,
          p.time_limit_ms, p.memory_limit_mb, p.max_output_size_kb, p.max_submissions_per_hour,
          p.current_testcase_set_version, s.spec_version, s.matching_strategy, s.epsilon_value,
          t.id AS testcase_id, t.visibility, t.position,
          b.object_key, b.testcase_count, b.content_sha256
        FROM problem.problems p LEFT JOIN problem.problem_specs s ON s.problem_id=p.id
        LEFT JOIN problem.testcases t ON t.problem_id=p.id AND t.approved=true
        LEFT JOIN problem.testcase_set_snapshots b
          ON b.problem_id=p.id AND b.version=p.current_testcase_set_version
        WHERE p.id=:id ORDER BY t.position, t.id
        """)
            .param("id", problemId)
            .query((rs, row) -> row(rs))
            .list();
    if (rows.isEmpty()) {
      return Optional.empty();
    }
    Row first = rows.getFirst();
    return Optional.of(
        new GradingReadSnapshot(
            first.data(),
            rows.stream().map(Row::testcase).filter(java.util.Objects::nonNull).toList(),
            first.snapshot()));
  }

  private static Row row(ResultSet rs) throws SQLException {
    UUID id = rs.getObject("id", UUID.class);
    int version = rs.getInt("current_testcase_set_version");
    var data =
        new GradingSpecData(
            new GradingSpecData.Problem(
                id,
                "PUBLISHED".equals(rs.getString("status")) && !rs.getBoolean("deleted"),
                rs.getBoolean("function_wrapper_supported")),
            new GradingSpecData.Limits(
                rs.getInt("time_limit_ms"),
                rs.getInt("memory_limit_mb"),
                rs.getInt("max_output_size_kb"),
                rs.getObject("max_submissions_per_hour", Integer.class)),
            new GradingSpecData.Versions(rs.getInt("spec_version"), version),
            new GradingSpecData.Matching(
                rs.getString("matching_strategy"), rs.getBigDecimal("epsilon_value")));
    UUID testcaseId = rs.getObject("testcase_id", UUID.class);
    var testcase =
        testcaseId == null
            ? null
            : new TestcaseDescriptor(testcaseId, rs.getString("visibility"), rs.getInt("position"));
    String key = rs.getString("object_key");
    var snapshot =
        key == null
            ? null
            : new SnapshotMetadata(
                id, version, key, rs.getInt("testcase_count"), rs.getString("content_sha256"));
    return new Row(data, testcase, snapshot);
  }

  @Override
  public List<StoredSampleTestcase> findSamples(UUID problemId) {
    return jdbc.getObject()
        .sql(
            """
        SELECT id, position, input_storage, input_inline, input_object_key,
          expected_output_storage, expected_output_inline, expected_output_object_key
        FROM problem.testcases WHERE problem_id=:id AND visibility='SAMPLE' AND approved=true
        ORDER BY position, id
        """)
        .param("id", problemId)
        .query(
            (rs, row) ->
                new StoredSampleTestcase(
                    rs.getObject("id", UUID.class),
                    rs.getInt("position"),
                    new StoredSampleTestcase.Content(
                        rs.getString("input_storage"),
                        rs.getString("input_inline"),
                        rs.getString("input_object_key")),
                    new StoredSampleTestcase.Content(
                        rs.getString("expected_output_storage"),
                        rs.getString("expected_output_inline"),
                        rs.getString("expected_output_object_key"))))
        .list();
  }

  @Override
  public Optional<SnapshotMetadata> findSnapshot(UUID problemId, int version) {
    return jdbc.getObject()
        .sql(
            """
        SELECT object_key, testcase_count, content_sha256 FROM problem.testcase_set_snapshots
        WHERE problem_id=:id AND version=:version
        """)
        .param("id", problemId)
        .param("version", version)
        .query(
            (rs, row) ->
                new SnapshotMetadata(
                    problemId,
                    version,
                    rs.getString("object_key"),
                    rs.getInt("testcase_count"),
                    rs.getString("content_sha256")))
        .optional();
  }

  private record Row(
      GradingSpecData data, TestcaseDescriptor testcase, SnapshotMetadata snapshot) {}
}
