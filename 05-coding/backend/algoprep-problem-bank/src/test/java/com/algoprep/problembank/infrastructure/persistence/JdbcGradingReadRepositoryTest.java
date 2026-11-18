package com.algoprep.problembank.infrastructure.persistence;

import static com.algoprep.problembank.infrastructure.persistence.FakeJdbc.row;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Grading read model (DD F2 grading read): one join row per approved testcase. */
class JdbcGradingReadRepositoryTest {
  private static final UUID P = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID T1 = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

  private final FakeJdbc db = new FakeJdbc();
  private final JdbcGradingReadRepository repo = new JdbcGradingReadRepository(db.provider());

  private Map<String, Object> joined(
      String status, boolean deleted, Object testcaseId, Object objectKey) {
    return row(
        "id",
        P,
        "status",
        status,
        "deleted",
        deleted,
        "function_wrapper_supported",
        true,
        "time_limit_ms",
        1000,
        "memory_limit_mb",
        256,
        "max_output_size_kb",
        64,
        "max_submissions_per_hour",
        null,
        "current_testcase_set_version",
        5,
        "spec_version",
        2,
        "matching_strategy",
        "FLOAT_TOLERANCE",
        "epsilon_value",
        new BigDecimal("0.01"),
        "testcase_id",
        testcaseId,
        "visibility",
        "SAMPLE",
        "position",
        1,
        "object_key",
        objectKey,
        "testcase_count",
        3,
        "content_sha256",
        "abc");
  }

  @Test
  void unknownProblemHasNoGradingSpec() {
    assertThat(repo.findGradingSpec(P)).isEmpty();
  }

  @Test
  void publishedProblemWithTestcasesAndSnapshotIsFullyMapped() {
    db.onQuery(
        "FROM problem.problems p LEFT JOIN problem.problem_specs s",
        joined("PUBLISHED", false, T1, "snap/key"),
        joined("PUBLISHED", false, UUID.randomUUID(), "snap/key"));

    var snap = repo.findGradingSpec(P).orElseThrow();

    assertThat(snap.data().problem().published()).isTrue();
    assertThat(snap.data().limits().maxSubmissionsPerHour()).isNull();
    assertThat(snap.data().versions().currentTestcaseSetVersion()).isEqualTo(5);
    assertThat(snap.data().matching().epsilonValue()).isEqualByComparingTo("0.01");
    assertThat(snap.testcases()).hasSize(2);
    assertThat(snap.testcases().get(0).testcaseId()).isEqualTo(T1);
    assertThat(snap.snapshot().objectKey()).isEqualTo("snap/key");
    assertThat(snap.snapshot().testcaseCount()).isEqualTo(3);
    assertThat(snap.snapshot().version()).isEqualTo(5);
  }

  @Test
  void problemWithoutTestcasesOrSnapshotYieldsEmptyListAndNullSnapshot() {
    db.onQuery(
        "FROM problem.problems p LEFT JOIN problem.problem_specs s",
        joined("DRAFT", false, null, null));

    var snap = repo.findGradingSpec(P).orElseThrow();

    assertThat(snap.data().problem().published()).isFalse();
    assertThat(snap.testcases()).isEmpty();
    assertThat(snap.snapshot()).isNull();
  }

  @Test
  void deletedPublishedProblemIsNotGradable() {
    db.onQuery(
        "FROM problem.problems p LEFT JOIN problem.problem_specs s",
        joined("PUBLISHED", true, null, null));

    assertThat(repo.findGradingSpec(P).orElseThrow().data().problem().published()).isFalse();
  }

  @Test
  void findSamplesMapsApprovedSampleContent() {
    db.onQuery(
        "visibility='SAMPLE'",
        row(
            "id",
            T1,
            "position",
            2,
            "input_storage",
            "INLINE",
            "input_inline",
            "1",
            "input_object_key",
            null,
            "expected_output_storage",
            "INLINE",
            "expected_output_inline",
            "2",
            "expected_output_object_key",
            null));

    var samples = repo.findSamples(P);

    assertThat(samples)
        .singleElement()
        .satisfies(
            s -> {
              assertThat(s.position()).isEqualTo(2);
              assertThat(s.input().inline()).isEqualTo("1");
              assertThat(s.expectedOutput().inline()).isEqualTo("2");
            });
    assertThat(repo.findSamples(UUID.randomUUID())).hasSize(1);
  }

  @Test
  void findSnapshotBindsVersionAndCarriesItBack() {
    assertThat(repo.findSnapshot(P, 9)).isEmpty();
    db.onQuery(
        "FROM problem.testcase_set_snapshots",
        row("object_key", "k", "testcase_count", 4, "content_sha256", "h"));

    var meta = repo.findSnapshot(P, 9).orElseThrow();

    assertThat(meta.version()).isEqualTo(9);
    assertThat(meta.problemId()).isEqualTo(P);
    assertThat(meta.contentSha256()).isEqualTo("h");
    assertThat(db.last("FROM problem.testcase_set_snapshots").param("version")).isEqualTo(9);
  }
}
