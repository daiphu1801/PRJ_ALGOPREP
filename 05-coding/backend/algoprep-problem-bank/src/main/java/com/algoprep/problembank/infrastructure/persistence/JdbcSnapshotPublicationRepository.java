package com.algoprep.problembank.infrastructure.persistence;

import com.algoprep.problembank.domain.model.SnapshotMetadata;
import com.algoprep.problembank.domain.model.SnapshotPublicationState;
import com.algoprep.problembank.domain.model.StoredGradingTestcase;
import com.algoprep.problembank.domain.model.TestcaseDescriptor;
import com.algoprep.problembank.domain.ports.out.SnapshotPublicationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcSnapshotPublicationRepository implements SnapshotPublicationRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcSnapshotPublicationRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<SnapshotPublicationState> lockProblem(UUID problemId) {
    JdbcClient client = jdbc.getObject();
    var version =
        client
            .sql(
                """
        SELECT p.current_testcase_set_version
        FROM problem.problems p WHERE p.id=:id AND NOT p.deleted FOR UPDATE OF p
        """)
            .param("id", problemId)
            .query(Integer.class)
            .optional();
    // A lock waiter must read metadata with a fresh READ COMMITTED statement snapshot.
    return version.map(
        current ->
            new SnapshotPublicationState(
                current,
                client
                    .sql(
                        """
        SELECT EXISTS(SELECT 1 FROM problem.testcase_set_snapshots
          WHERE problem_id=:id AND version=:version)
        """)
                    .param("id", problemId)
                    .param("version", current)
                    .query(Boolean.class)
                    .single()));
  }

  @Override
  public List<StoredGradingTestcase> findApproved(UUID problemId) {
    return jdbc.getObject()
        .sql(
            """
        SELECT t.id, t.visibility, t.position, t.input_storage, t.input_inline, t.input_object_key,
          t.expected_output_storage, t.expected_output_inline, t.expected_output_object_key
        FROM problem.testcases t WHERE t.problem_id=:id AND t.approved=true
        ORDER BY t.position, t.id FOR SHARE OF t
        """)
        .param("id", problemId)
        .query(
            (rs, row) ->
                new StoredGradingTestcase(
                    new TestcaseDescriptor(
                        rs.getObject("id", UUID.class),
                        rs.getString("visibility"),
                        rs.getInt("position")),
                    new StoredGradingTestcase.Content(
                        rs.getString("input_storage"),
                        rs.getString("input_inline"),
                        rs.getString("input_object_key")),
                    new StoredGradingTestcase.Content(
                        rs.getString("expected_output_storage"),
                        rs.getString("expected_output_inline"),
                        rs.getString("expected_output_object_key"))))
        .list();
  }

  @Override
  public void insertSnapshot(SnapshotMetadata metadata) {
    jdbc.getObject()
        .sql(
            """
        INSERT INTO problem.testcase_set_snapshots(problem_id,version,object_key,testcase_count,content_sha256)
        VALUES (:id,:version,:key,:count,:sha)
        """)
        .param("id", metadata.problemId())
        .param("version", metadata.version())
        .param("key", metadata.objectKey())
        .param("count", metadata.testcaseCount())
        .param("sha", metadata.contentSha256())
        .update();
  }

  @Override
  public Instant bumpVersion(UUID problemId, int expectedVersion, int nextVersion) {
    return jdbc.getObject()
        .sql(
            """
        UPDATE problem.problems SET current_testcase_set_version=:next,
          updated_at=GREATEST(clock_timestamp(),updated_at + interval '1 microsecond')
        WHERE id=:id AND current_testcase_set_version=:expected AND NOT deleted
        RETURNING updated_at
        """)
        .param("id", problemId)
        .param("expected", expectedVersion)
        .param("next", nextVersion)
        .query((rs, row) -> rs.getTimestamp("updated_at").toInstant())
        .single();
  }

  @Override
  public void appendBumpEvent(UUID problemId, String envelope) {
    jdbc.getObject()
        .sql(
            """
        INSERT INTO problem.outbox_events(id,aggregate_type,aggregate_id,event_type,routing_key,payload)
        VALUES (CAST(CAST(:envelope AS jsonb)->>'eventId' AS uuid),'Problem',:id,
          'TestcaseSetVersionBumped','problem.testcase-set.version-bumped',CAST(:envelope AS jsonb))
        """)
        .param("id", problemId)
        .param("envelope", envelope)
        .update();
  }
}
