package com.algoprep.problembank.infrastructure.persistence;

import static com.algoprep.problembank.infrastructure.persistence.FakeJdbc.row;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.problembank.domain.model.StoredGradingTestcase.Content;
import com.algoprep.problembank.domain.model.StoredTestcase;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;

/** Testcase write port: counts, positions, row lock mapping, parameter binding. */
class JdbcTestcaseWriteRepositoryTest {
  private static final UUID PROBLEM = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID TC = UUID.fromString("00000000-0000-0000-0000-000000000002");

  private final FakeJdbc db = new FakeJdbc();
  private final JdbcTestcaseWriteRepository repo = new JdbcTestcaseWriteRepository(db.provider());

  private static StoredTestcase testcase() {
    return new StoredTestcase(
        TC,
        "HIDDEN",
        "MANUAL",
        true,
        "EDGE",
        3,
        new Content("INLINE", "1 2", null),
        new Content("OBJECT", null, "k/out"));
  }

  @Test
  void countsMapsTotalApprovedAndApprovedSamples() {
    db.onQuery("AS samples", row("total", 5, "approved", 3, "samples", 2));

    var counts = repo.counts(PROBLEM);

    assertThat(counts.total()).isEqualTo(5);
    assertThat(counts.approved()).isEqualTo(3);
    assertThat(counts.approvedSamples()).isEqualTo(2);
  }

  @Test
  void nextPositionComesFromMaxPlusOne() {
    db.onQuery("max(position)", 8);

    assertThat(repo.nextPosition(PROBLEM)).isEqualTo(8);
  }

  @Test
  void lockMapsBothContentSidesAndIsEmptyWhenMissing() {
    assertThat(repo.lock(PROBLEM, TC)).isEmpty();
    db.onQuery(
        "FOR UPDATE",
        row(
            "id",
            TC,
            "visibility",
            "SAMPLE",
            "origin",
            "AI",
            "approved",
            false,
            "category",
            null,
            "position",
            2,
            "input_storage",
            "INLINE",
            "input_inline",
            "x",
            "input_object_key",
            null,
            "expected_output_storage",
            "OBJECT",
            "expected_output_inline",
            null,
            "expected_output_object_key",
            "k"));

    var found = repo.lock(PROBLEM, TC).orElseThrow();

    assertThat(found.visibility()).isEqualTo("SAMPLE");
    assertThat(found.approved()).isFalse();
    assertThat(found.category()).isNull();
    assertThat(found.input().inline()).isEqualTo("x");
    assertThat(found.expectedOutput().objectKey()).isEqualTo("k");
    assertThat(db.last("FOR UPDATE").param("problem")).isEqualTo(PROBLEM);
  }

  @Test
  void insertAndUpdateBindEveryColumn() {
    repo.insert(PROBLEM, testcase());
    repo.update(testcase());

    var insert = db.last("INSERT INTO problem.testcases");
    assertThat(insert.param("problem")).isEqualTo(PROBLEM);
    assertThat(insert.param("inStorage")).isEqualTo("INLINE");
    assertThat(insert.param("inInline")).isEqualTo("1 2");
    assertThat(insert.param("outKey")).isEqualTo("k/out");
    assertThat(insert.param("origin")).isEqualTo("MANUAL");
    var update = db.last("UPDATE problem.testcases SET visibility");
    assertThat(update.param("position")).isEqualTo(3);
    assertThat(update.param("approved")).isEqualTo(true);
  }

  @Test
  void deleteAndLockOrderTargetTheGivenIds() {
    UUID other = UUID.randomUUID();
    db.onQuery("ORDER BY position, id FOR UPDATE", TC, other);

    repo.delete(TC);

    assertThat(db.last("DELETE FROM problem.testcases").param("id")).isEqualTo(TC);
    assertThat(repo.lockOrder(PROBLEM)).containsExactly(TC, other);
  }

  @Test
  void setPositionsRenumbersFromOneAndEmptyListWritesNothing() {
    UUID second = UUID.randomUUID();

    repo.setPositions(List.of(TC, second));

    var updates = db.calls();
    assertThat(updates).hasSize(2);
    assertThat(updates.get(0).param("position")).isEqualTo(1);
    assertThat(updates.get(0).param("id")).isEqualTo(TC);
    assertThat(updates.get(1).param("position")).isEqualTo(2);

    var empty = new FakeJdbc();
    new JdbcTestcaseWriteRepository(empty.provider()).setPositions(List.of());
    assertThat(empty.calls()).isEmpty();
  }

  @Test
  void touchAndStateMapVersionAndTimestamp() {
    Instant stamp = Instant.parse("2026-05-06T07:08:09Z");
    db.onQuery("RETURNING updated_at", row("updated_at", stamp, "current_testcase_set_version", 4))
        .onQuery(
            "SELECT updated_at, current_testcase_set_version",
            row("updated_at", stamp, "current_testcase_set_version", 2));
    UUID actor = UUID.randomUUID();

    var touched = repo.touch(PROBLEM, actor);
    var state = repo.state(PROBLEM);

    assertThat(touched.testcaseSetVersion()).isEqualTo(4);
    assertThat(touched.updatedAt()).isEqualTo(stamp);
    assertThat(state.testcaseSetVersion()).isEqualTo(2);
    assertThat(db.last("RETURNING updated_at").param("actor")).isEqualTo(actor);
  }

  @Test
  void touchOfVanishedProblemFailsInsteadOfReturningNull() {
    assertThatThrownBy(() -> repo.touch(PROBLEM, UUID.randomUUID()))
        .isInstanceOf(EmptyResultDataAccessException.class);
  }
}
