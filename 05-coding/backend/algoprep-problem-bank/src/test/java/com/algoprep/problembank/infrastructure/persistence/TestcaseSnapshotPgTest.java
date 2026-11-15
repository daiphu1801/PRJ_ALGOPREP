package com.algoprep.problembank.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.problembank.domain.model.SnapshotMetadata;
import com.algoprep.problembank.domain.model.StoredGradingTestcase.Content;
import com.algoprep.problembank.domain.model.StoredTestcase;
import com.algoprep.problembank.domain.model.TestcaseDescriptor;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;

/** Testcase authoring, version bump, snapshot publication and grading reads on real PostgreSQL. */
class TestcaseSnapshotPgTest extends PostgresTestBase {
  private JdbcTestcaseWriteRepository write;
  private JdbcSnapshotPublicationRepository snapshots;
  private JdbcGradingReadRepository grading;
  private UUID p;

  @BeforeEach
  void seed() {
    write = new JdbcTestcaseWriteRepository(provider());
    snapshots = new JdbcSnapshotPublicationRepository(provider());
    grading = new JdbcGradingReadRepository(provider());
    p = published(1, "P-ONE");
  }

  private static StoredTestcase inline(long n, String visibility, boolean approved, int pos) {
    return new StoredTestcase(
        id(n),
        visibility,
        "MANUAL",
        approved,
        "TYPICAL",
        pos,
        new Content("INLINE", "in" + n, null),
        new Content("INLINE", "out" + n, null));
  }

  // ---- authoring writes ------------------------------------------------------------------------

  @Test
  void insertWritesBothStorageFormsAndCountsFollow() {
    write.insert(p, inline(100, "SAMPLE", true, write.nextPosition(p)));
    write.insert(
        p,
        new StoredTestcase(
            id(101),
            "HIDDEN",
            "AI",
            false,
            null,
            write.nextPosition(p),
            new Content("MINIO", null, "tc/in101"),
            new Content("MINIO", null, "tc/out101")));

    var counts = write.counts(p);

    assertThat(counts.total()).isEqualTo(2);
    assertThat(counts.approved()).isEqualTo(1);
    assertThat(counts.approvedSamples()).isEqualTo(1);
    assertThat(write.nextPosition(p)).isEqualTo(3);
    var stored = write.lock(p, id(101)).orElseThrow();
    assertThat(stored.input().objectKey()).isEqualTo("tc/in101");
    assertThat(stored.input().inline()).isNull();
    assertThat(stored.category()).isNull();
    assertThat(write.lock(id(999), id(101))).isEmpty();
  }

  @Test
  void checkConstraintsRejectInconsistentStorageAndUnknownEnums() {
    var bothSides =
        new StoredTestcase(
            id(100),
            "SAMPLE",
            "MANUAL",
            true,
            null,
            1,
            new Content("INLINE", "x", "also-key"),
            new Content("INLINE", "y", null));
    var noContent =
        new StoredTestcase(
            id(101),
            "SAMPLE",
            "MANUAL",
            true,
            null,
            1,
            new Content("INLINE", null, null),
            new Content("INLINE", "y", null));
    var badVisibility = inline(102, "PRIVATE", true, 1);
    var badPosition = inline(103, "HIDDEN", true, 0);
    var badCategory =
        new StoredTestcase(
            id(104),
            "HIDDEN",
            "MANUAL",
            true,
            "WEIRD",
            1,
            new Content("INLINE", "x", null),
            new Content("INLINE", "y", null));

    for (var invalid : List.of(bothSides, noContent, badVisibility, badPosition, badCategory)) {
      assertThatThrownBy(() -> write.insert(p, invalid))
          .isInstanceOf(DataIntegrityViolationException.class);
    }
    assertThat(count("testcases")).isZero();
  }

  @Test
  void emptyInlineStringIsValidDataAndTestcaseOfUnknownProblemViolatesForeignKey() {
    write.insert(
        p,
        new StoredTestcase(
            id(100),
            "HIDDEN",
            "MANUAL",
            true,
            null,
            1,
            new Content("INLINE", "", null),
            new Content("INLINE", "", null)));

    assertThat(write.lock(p, id(100)).orElseThrow().input().inline()).isEmpty();
    assertThatThrownBy(() -> write.insert(id(999), inline(101, "HIDDEN", true, 1)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void updateDeleteAndReorderPersistTheNewState() {
    write.insert(p, inline(100, "HIDDEN", false, 1));
    write.insert(p, inline(101, "HIDDEN", true, 2));
    write.insert(p, inline(102, "HIDDEN", true, 3));

    write.update(inline(100, "SAMPLE", true, 1));
    write.delete(id(101));
    write.setPositions(List.of(id(102), id(100)));

    assertThat(write.lockOrder(p)).containsExactly(id(102), id(100));
    var updated = write.lock(p, id(100)).orElseThrow();
    assertThat(updated.visibility()).isEqualTo("SAMPLE");
    assertThat(updated.approved()).isTrue();
    assertThat(updated.position()).isEqualTo(2);
    assertThat(count("testcases", "id = '" + id(101) + "'")).isZero();
  }

  @Test
  void touchBumpsUpdatedAtStrictlyAndRejectsDeletedProblem() {
    var before = write.state(p);

    var first = write.touch(p, id(900));
    var second = write.touch(p, id(901));

    assertThat(first.updatedAt()).isAfter(before.updatedAt());
    assertThat(second.updatedAt()).isAfter(first.updatedAt());
    assertThat(second.testcaseSetVersion()).isEqualTo(1);
    assertThat(
            jdbc.sql("SELECT updated_by FROM problem.problems WHERE id=:id")
                .param("id", p)
                .query(UUID.class)
                .single())
        .isEqualTo(id(901));
    softDelete(p);
    assertThatThrownBy(() -> write.touch(p, id(900)))
        .isInstanceOf(EmptyResultDataAccessException.class);
  }

  // ---- snapshot publication and versioning
  // -------------------------------------------------------

  @Test
  void publicationFlowLocksReadsOnlyApprovedBumpsVersionAndWritesOutbox() {
    testcase(p, 100, "HIDDEN", true, at(2, "b"));
    testcase(p, 101, "SAMPLE", true, at(1, "a"));
    testcase(p, 102, "HIDDEN", false, at(3, "unapproved"));

    var state = snapshots.lockProblem(p).orElseThrow();
    assertThat(state.version()).isEqualTo(1);
    assertThat(state.snapshotExists()).isFalse();

    var approved = snapshots.findApproved(p);
    assertThat(approved)
        .extracting(t -> t.descriptor().testcaseId())
        .containsExactly(id(101), id(100));

    snapshots.insertSnapshot(
        new SnapshotMetadata(p, 1, "snap/p/1", approved.size(), "a".repeat(64)));
    assertThat(snapshots.lockProblem(p).orElseThrow().snapshotExists()).isTrue();

    var bumped = snapshots.bumpVersion(p, 1, 2);
    String envelope = "{\"eventId\":\"" + id(700) + "\",\"version\":2}";
    snapshots.appendBumpEvent(p, envelope);

    assertThat(bumped).isNotNull();
    assertThat(write.state(p).testcaseSetVersion()).isEqualTo(2);
    assertThat(snapshots.lockProblem(p).orElseThrow().snapshotExists()).isFalse();
    var outbox =
        jdbc.sql(
                "SELECT event_type, routing_key, aggregate_id, publish_attempts FROM"
                    + " problem.outbox_events WHERE id=:id")
            .param("id", id(700))
            .query(
                (rs, n) ->
                    rs.getString(1)
                        + "|"
                        + rs.getString(2)
                        + "|"
                        + rs.getObject(3, UUID.class)
                        + "|"
                        + rs.getInt(4))
            .single();
    assertThat(outbox)
        .isEqualTo("TestcaseSetVersionBumped|problem.testcase-set.version-bumped|" + p + "|0");
    assertThat(snapshots.lockProblem(id(999))).isEmpty();
    softDelete(p);
    assertThat(snapshots.lockProblem(p)).isEmpty();
  }

  @Test
  void staleExpectedVersionLeavesTheProblemUntouched() {
    assertThatThrownBy(() -> snapshots.bumpVersion(p, 5, 6))
        .isInstanceOf(EmptyResultDataAccessException.class);

    assertThat(write.state(p).testcaseSetVersion()).isEqualTo(1);
  }

  @Test
  void snapshotConstraintsRejectDuplicatesAndEmptySets() {
    snapshots.insertSnapshot(new SnapshotMetadata(p, 1, "snap/1", 2, "a".repeat(64)));

    assertThatThrownBy(
            () -> snapshots.insertSnapshot(new SnapshotMetadata(p, 1, "snap/x", 2, "a".repeat(64))))
        .isInstanceOf(DuplicateKeyException.class);
    assertThatThrownBy(
            () -> snapshots.insertSnapshot(new SnapshotMetadata(p, 2, "snap/1", 2, "a".repeat(64))))
        .isInstanceOf(DuplicateKeyException.class);
    assertThatThrownBy(
            () -> snapshots.insertSnapshot(new SnapshotMetadata(p, 3, "snap/3", 0, "a".repeat(64))))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () -> snapshots.insertSnapshot(new SnapshotMetadata(p, 0, "snap/0", 1, "a".repeat(64))))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                snapshots.insertSnapshot(
                    new SnapshotMetadata(id(999), 1, "snap/9", 1, "a".repeat(64))))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(count("testcase_set_snapshots")).isEqualTo(1);
  }

  @Test
  void duplicateBumpEventIdIsRejectedByThePrimaryKey() {
    String envelope = "{\"eventId\":\"" + id(700) + "\"}";
    snapshots.appendBumpEvent(p, envelope);

    assertThatThrownBy(() -> snapshots.appendBumpEvent(p, envelope))
        .isInstanceOf(DuplicateKeyException.class);
    assertThat(count("outbox_events")).isEqualTo(1);
  }

  // ---- grading read ----------------------------------------------------------------------------

  @Test
  void gradingSpecJoinsApprovedTestcasesAndTheCurrentVersionSnapshot() {
    testcase(p, 100, "HIDDEN", true, at(2, "h"));
    testcase(p, 101, "SAMPLE", true, at(1, "s"));
    testcase(p, 102, "HIDDEN", false, at(3, "draft"));
    jdbc.sql(
            "INSERT INTO problem.problem_specs(id,problem_id,matching_strategy,epsilon_value,spec_version)"
                + " VALUES(:id,:p,'EPSILON',0.001,4)")
        .param("id", id(61))
        .param("p", p)
        .update();
    snapshots.insertSnapshot(new SnapshotMetadata(p, 1, "snap/1", 2, "b".repeat(64)));
    snapshots.insertSnapshot(new SnapshotMetadata(p, 2, "snap/2", 9, "c".repeat(64)));

    var found = grading.findGradingSpec(p).orElseThrow();

    assertThat(found.data().problem().published()).isTrue();
    assertThat(found.data().versions().specVersion()).isEqualTo(4);
    assertThat(found.data().limits().timeLimitMs()).isEqualTo(1000);
    assertThat(found.data().limits().maxSubmissionsPerHour()).isNull();
    assertThat(found.data().matching().epsilonValue()).isEqualByComparingTo("0.001");
    assertThat(found.testcases())
        .extracting(TestcaseDescriptor::testcaseId)
        .containsExactly(id(101), id(100));
    assertThat(found.snapshot().objectKey()).isEqualTo("snap/1");
    assertThat(grading.findSnapshot(p, 2).orElseThrow().testcaseCount()).isEqualTo(9);
    assertThat(grading.findSnapshot(p, 3)).isEmpty();
    assertThat(grading.findGradingSpec(id(999))).isEmpty();
  }

  @Test
  void gradingSpecOfProblemWithoutTestcasesOrSnapshotHasEmptyParts() {
    var found = grading.findGradingSpec(p).orElseThrow();

    assertThat(found.testcases()).isEmpty();
    assertThat(found.snapshot()).isNull();
    assertThat(found.data().versions().specVersion()).isZero();
    softDelete(p);
    assertThat(grading.findGradingSpec(p).orElseThrow().data().problem().published()).isFalse();
  }

  @Test
  void findSamplesReturnsApprovedSamplesOnly() {
    testcase(p, 100, "SAMPLE", true, at(2, "second"));
    testcase(p, 101, "SAMPLE", true, at(1, "first"));
    testcase(p, 102, "SAMPLE", false, at(3, "draft"));
    testcase(p, 103, "HIDDEN", true, at(4, "SECRET"));

    var samples = grading.findSamples(p);

    assertThat(samples).extracting(s -> s.testcaseId()).containsExactly(id(101), id(100));
    assertThat(samples.toString()).doesNotContain("SECRET");
  }
}
