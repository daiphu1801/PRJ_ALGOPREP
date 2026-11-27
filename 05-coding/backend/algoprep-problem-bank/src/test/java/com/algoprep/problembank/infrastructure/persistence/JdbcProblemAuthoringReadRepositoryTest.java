package com.algoprep.problembank.infrastructure.persistence;

import static com.algoprep.problembank.infrastructure.persistence.FakeJdbc.row;
import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Authoring read (DD F2 section 34): privacy flag reaches SQL, optional parts mapped. */
class JdbcProblemAuthoringReadRepositoryTest {
  private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID LEVEL = UUID.fromString("00000000-0000-0000-0000-0000000000c1");

  private final FakeJdbc db = new FakeJdbc();
  private final IdGeneratorPort ids = UUID::randomUUID;
  private final JdbcProblemAuthoringReadRepository repo =
      new JdbcProblemAuthoringReadRepository(
          db.provider(), new JdbcProblemContentRelations(db.provider(), ids));

  private Object problemRow(Object publishedAt, Object quota) {
    return row(
        "code",
        "TWO-SUM",
        "title",
        "Two Sum",
        "statement_md",
        "s",
        "constraints_md",
        "c",
        "level_id",
        LEVEL,
        "status",
        "PUBLISHED",
        "function_wrapper_supported",
        true,
        "time_limit_ms",
        1000,
        "memory_limit_mb",
        256,
        "max_output_size_kb",
        64,
        "max_submissions_per_hour",
        quota,
        "current_testcase_set_version",
        3,
        "ai_testcase_generations_used",
        1,
        "ai_brief",
        "brief",
        "no_full_code",
        true,
        "socratic_only",
        false,
        "author_id",
        ID,
        "updated_by",
        ID,
        "duplicated_from_problem_id",
        null,
        "updated_at",
        Instant.EPOCH,
        "published_at",
        publishedAt);
  }

  @Test
  void findReturnsEmptyForMissingOrDeletedProblem() {
    assertThat(repo.find(ID, false)).isEmpty();
  }

  @Test
  void findMapsEverythingAndPassesContentFlagToTestcaseQuery() {
    UUID topic = UUID.randomUUID();
    UUID tagId = UUID.randomUUID();
    db.onQuery(
            "SELECT * FROM problem.problems", problemRow(Instant.parse("2026-01-01T00:00:00Z"), 7))
        .onQuery("SELECT topic_id", topic)
        .onQuery("SELECT t.id, t.name", row("id", tagId, "name", "dp"))
        .onQuery(
            "AS expected_object_key",
            row(
                "id", ID,
                "position", 1,
                "visibility", "SAMPLE",
                "origin", "MANUAL",
                "approved", true,
                "category", "BASIC",
                "input_storage", "INLINE",
                "input_inline", "in",
                "input_object_key", null,
                "expected_output_storage", "INLINE",
                "expected_inline", "out",
                "expected_object_key", null))
        .onQuery(
            "SELECT * FROM problem.problem_specs",
            row(
                "matching_strategy", "EXACT",
                "epsilon_value", new BigDecimal("0.001"),
                "stdin_format_md", "a",
                "stdout_format_md", "b",
                "function_name", "twoSum",
                "return_type", "{}",
                "parameters", "[]",
                "name_overrides", null,
                "spec_version", 6));

    var rec = repo.find(ID, true).orElseThrow();

    assertThat(rec.code()).isEqualTo("TWO-SUM");
    assertThat(rec.limits().maxSubmissionsPerHour()).isEqualTo(7);
    assertThat(rec.publishedAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    assertThat(rec.topicIds()).containsExactly(topic);
    assertThat(rec.tags()).singleElement().extracting("name").isEqualTo("dp");
    assertThat(rec.testcases()).singleElement().extracting("inputInline").isEqualTo("in");
    assertThat(rec.spec().specVersion()).isEqualTo(6);
    assertThat(rec.spec().data().epsilonValue()).isEqualByComparingTo("0.001");
    assertThat(db.last("AS expected_object_key").param("content")).isEqualTo(true);
    assertThat(rec.aiGenerationsUsed()).isEqualTo(1);
    assertThat(rec.currentTestcaseSetVersion()).isEqualTo(3);
  }

  @Test
  void findWithoutContentAndWithoutSpecOrPublishDate() {
    db.onQuery("SELECT * FROM problem.problems", problemRow(null, null));

    var rec = repo.find(ID, false).orElseThrow();

    assertThat(rec.publishedAt()).isNull();
    assertThat(rec.limits().maxSubmissionsPerHour()).isNull();
    assertThat(rec.spec()).isNull();
    assertThat(rec.testcases()).isEmpty();
    assertThat(db.last("AS expected_object_key").param("content")).isEqualTo(false);
  }

  @Test
  void aiGenerationLimitReadsSingletonSettingsRow() {
    db.onQuery("max_ai_generations_per_problem", 12);

    assertThat(repo.aiGenerationLimit()).isEqualTo(12);
  }
}
