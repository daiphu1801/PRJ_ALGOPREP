package com.algoprep.problembank.infrastructure.persistence;

import static com.algoprep.problembank.infrastructure.persistence.FakeJdbc.row;
import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.problembank.domain.model.snapshot.SnapshotMetadata;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Snapshot publication persistence: lock then fresh metadata read, optimistic version bump. */
class JdbcSnapshotPublicationRepositoryTest {
  private static final UUID P = UUID.fromString("00000000-0000-0000-0000-000000000001");

  private final FakeJdbc db = new FakeJdbc();
  private final JdbcSnapshotPublicationRepository repo =
      new JdbcSnapshotPublicationRepository(db.provider());

  @Test
  void lockProblemIsEmptyForMissingProblemAndDoesNotReadSnapshots() {
    assertThat(repo.lockProblem(P)).isEmpty();
    assertThat(db.count("testcase_set_snapshots")).isZero();
  }

  @Test
  void lockProblemReportsCurrentVersionAndWhetherItsSnapshotExists() {
    db.onQuery("FOR UPDATE OF p", 3).onQuery("SELECT EXISTS", true);

    var state = repo.lockProblem(P).orElseThrow();

    assertThat(state.version()).isEqualTo(3);
    assertThat(state.snapshotExists()).isTrue();
    assertThat(db.last("SELECT EXISTS").param("version")).isEqualTo(3);
    assertThat(db.sqls().get(0)).contains("FOR UPDATE OF p");
  }

  @Test
  void findApprovedMapsDescriptorAndContentUnderSharedLock() {
    UUID tc = UUID.randomUUID();
    db.onQuery(
        "FOR SHARE OF t",
        row(
            "id", tc,
            "visibility", "HIDDEN",
            "position", 4,
            "input_storage", "OBJECT",
            "input_inline", null,
            "input_object_key", "in/k",
            "expected_output_storage", "INLINE",
            "expected_output_inline", "42",
            "expected_output_object_key", null));

    var found = repo.findApproved(P);

    assertThat(found)
        .singleElement()
        .satisfies(
            t -> {
              assertThat(t.descriptor().testcaseId()).isEqualTo(tc);
              assertThat(t.descriptor().position()).isEqualTo(4);
              assertThat(t.input().objectKey()).isEqualTo("in/k");
              assertThat(t.expectedOutput().inline()).isEqualTo("42");
            });
    assertThat(db.last("FOR SHARE OF t").sql()).contains("t.approved=true");
  }

  @Test
  void findApprovedOfProblemWithoutApprovedTestcasesIsEmpty() {
    assertThat(repo.findApproved(P)).isEmpty();
  }

  @Test
  void insertSnapshotBindsAllMetadata() {
    repo.insertSnapshot(new SnapshotMetadata(P, 2, "snap/2", 6, "sha"));

    var call = db.last("INSERT INTO problem.testcase_set_snapshots");
    assertThat(call.param("version")).isEqualTo(2);
    assertThat(call.param("key")).isEqualTo("snap/2");
    assertThat(call.param("count")).isEqualTo(6);
    assertThat(call.param("sha")).isEqualTo("sha");
  }

  @Test
  void bumpVersionComparesExpectedAndReturnsNewTimestamp() {
    Instant stamp = Instant.parse("2026-08-01T00:00:00Z");
    db.onQuery("RETURNING updated_at", row("updated_at", stamp));

    assertThat(repo.bumpVersion(P, 2, 3)).isEqualTo(stamp);
    var call = db.last("RETURNING updated_at");
    assertThat(call.param("expected")).isEqualTo(2);
    assertThat(call.param("next")).isEqualTo(3);
  }

  @Test
  void appendBumpEventTargetsTheVersionBumpedRoutingKey() {
    repo.appendBumpEvent(P, "{\"eventId\":\"e\"}");

    var call = db.last("INSERT INTO problem.outbox_events");
    assertThat(call.sql()).contains("'problem.testcase-set.version-bumped'");
    assertThat(call.param("envelope")).isEqualTo("{\"eventId\":\"e\"}");
  }
}
