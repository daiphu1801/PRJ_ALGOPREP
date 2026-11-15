package com.algoprep.problembank.infrastructure.persistence;

import static com.algoprep.problembank.infrastructure.persistence.FakeJdbc.row;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.problembank.domain.model.problem.ProblemContentData;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Problem content writes: relation replacement order, code allocation retries, lock mapping. */
class JdbcProblemContentRepositoryTest {
  private static final UUID PROBLEM = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
  private static final UUID LEVEL = UUID.fromString("00000000-0000-0000-0000-0000000000c1");

  private final FakeJdbc db = new FakeJdbc();
  private final AtomicInteger seq = new AtomicInteger();
  private final IdGeneratorPort ids = () -> new UUID(0, 1000 + seq.incrementAndGet());
  private final JdbcProblemContentRelations relations =
      new JdbcProblemContentRelations(db.provider(), ids);
  private final JdbcProblemContentRepository repo =
      new JdbcProblemContentRepository(db.provider(), ids, relations);

  private static ProblemContentData content(
      List<UUID> topics,
      List<String> tags,
      List<ProblemContentData.Example> examples,
      ProblemContentData.Solution solution) {
    return new ProblemContentData(
        "Two Sum",
        "stmt",
        "cons",
        LEVEL,
        new ProblemContentData.Limits(1000, 256, 64, null),
        topics,
        tags,
        examples,
        solution);
  }

  @Test
  void readsRelationsInQueryOrder() {
    UUID topic = UUID.randomUUID();
    db.onQuery("FROM problem.problem_topics", topic)
        .onQuery("FROM problem.tags t", "Hash", "array")
        .onQuery(
            "FROM problem.worked_examples",
            row("id", PROBLEM, "position", 1, "input", "i", "output", "o", "explanation_md", null))
        .onQuery(
            "FROM problem.problem_reference_solutions",
            row("language", "PYTHON", "source_code", "print()"));

    assertThat(relations.topics(PROBLEM)).containsExactly(topic);
    assertThat(relations.tags(PROBLEM)).containsExactly("Hash", "array");
    assertThat(relations.examples(PROBLEM)).singleElement().extracting("input").isEqualTo("i");
    assertThat(relations.solution(PROBLEM).language()).isEqualTo("PYTHON");
  }

  @Test
  void solutionIsNullWhenAbsent() {
    assertThat(relations.solution(PROBLEM)).isNull();
    assertThat(relations.examples(PROBLEM)).isEmpty();
  }

  @Test
  void replaceDeletesBeforeInsertingAndOrdersTagsCaseInsensitively() {
    UUID t1 = UUID.randomUUID();
    UUID t2 = UUID.randomUUID();
    db.onQuery("INSERT INTO problem.tags", c -> List.of(UUID.randomUUID()));
    var example = new ProblemContentData.Example(null, 9, "in", "out", "why");

    relations.replace(
        PROBLEM,
        content(
            List.of(t1, t2),
            List.of("beta", "Alpha", "alpha"),
            List.of(example),
            new ProblemContentData.Solution("JAVA", "class A {}")));

    var sqls = db.sqls();
    assertThat(sqls.get(0)).startsWith("DELETE FROM problem.problem_topics");
    assertThat(db.count("INSERT INTO problem.problem_topics")).isEqualTo(2);
    var tagNames = new ArrayList<Object>();
    db.calls().stream()
        .filter(c -> c.sql().startsWith("INSERT INTO problem.tags"))
        .forEach(c -> tagNames.add(c.param("name")));
    assertThat(tagNames).containsExactly("Alpha", "alpha", "beta");
    var insertExample = db.last("INSERT INTO problem.worked_examples");
    assertThat(insertExample.param("position")).isEqualTo(1);
    assertThat(insertExample.param("id")).isInstanceOf(UUID.class);
    assertThat(db.last("INSERT INTO problem.problem_reference_solutions").param("language"))
        .isEqualTo("JAVA");
    assertThat(db.count("DELETE FROM problem.problem_reference_solutions")).isZero();
  }

  @Test
  void replaceWithoutSolutionDeletesTheStoredOne() {
    var kept = new ProblemContentData.Example(PROBLEM, 1, "i", "o", null);

    relations.replace(PROBLEM, content(List.of(), List.of(), List.of(kept), null));

    assertThat(db.count("DELETE FROM problem.problem_reference_solutions")).isEqualTo(1);
    assertThat(db.last("INSERT INTO problem.worked_examples").param("id")).isEqualTo(PROBLEM);
    assertThat(db.count("INSERT INTO problem.problem_reference_solutions")).isZero();
  }

  @Test
  void lockMapsProblemRowAndRelations() {
    UUID level = LEVEL;
    db.onQuery(
        "FOR UPDATE",
        row(
            "code",
            "TWO-SUM",
            "author_id",
            ACTOR,
            "status",
            "DRAFT",
            "title",
            "Two Sum",
            "statement_md",
            "s",
            "constraints_md",
            null,
            "level_id",
            level,
            "time_limit_ms",
            2000,
            "memory_limit_mb",
            128,
            "max_output_size_kb",
            32,
            "max_submissions_per_hour",
            5,
            "updated_at",
            Instant.parse("2026-02-03T04:05:06Z")));

    var snap = repo.lock(PROBLEM).orElseThrow();

    assertThat(snap.code()).isEqualTo("TWO-SUM");
    assertThat(snap.content().limits().maxSubmissionsPerHour()).isEqualTo(5);
    assertThat(snap.content().limits().timeLimitMs()).isEqualTo(2000);
    assertThat(snap.content().referenceSolution()).isNull();
    assertThat(snap.updatedAt()).isEqualTo(Instant.parse("2026-02-03T04:05:06Z"));
  }

  @Test
  void lockOfMissingOrDeletedProblemIsEmpty() {
    assertThat(repo.lock(PROBLEM)).isEmpty();
  }

  @Test
  void lockCataloguesTakesBothAdvisoryLocksInFixedOrder() {
    db.onQuery("pg_advisory_xact_lock", row("v", true));

    repo.lockCatalogues();

    assertThat(db.sqls()).hasSize(2);
    assertThat(db.sqls().get(0)).contains("2201001");
    assertThat(db.sqls().get(1)).contains("2201002");
  }

  @Test
  void firstLevelIsEmptyWithoutLevels() {
    assertThat(repo.firstLevel()).isEmpty();
    db.onQuery("FROM problem.problem_levels ORDER BY", LEVEL);
    assertThat(repo.firstLevel()).contains(LEVEL);
  }

  @Test
  void cataloguesExistChecksLevelThenEveryTopicAndStopsAtFirstMissing() {
    UUID a = UUID.randomUUID();
    UUID b = UUID.randomUUID();
    db.onQuery("FROM problem.problem_levels WHERE id", false);
    assertThat(repo.cataloguesExist(LEVEL, List.of(a))).isFalse();
    assertThat(db.count("FROM problem.topics")).isZero();

    var db2 = new FakeJdbc();
    db2.onQuery("FROM problem.problem_levels WHERE id", true)
        .onQuery("FROM problem.topics WHERE id", c -> List.of(c.param("id").equals(a)));
    var repo2 =
        new JdbcProblemContentRepository(
            db2.provider(), ids, new JdbcProblemContentRelations(db2.provider(), ids));
    assertThat(repo2.cataloguesExist(LEVEL, List.of(a, b, UUID.randomUUID()))).isFalse();
    assertThat(db2.count("FROM problem.topics")).isEqualTo(2);
    assertThat(repo2.cataloguesExist(LEVEL, List.of(a))).isTrue();
    assertThat(repo2.cataloguesExist(LEVEL, List.of())).isTrue();
  }

  @Test
  void createAllocatesCodeOnFirstSuccessfulInsert() {
    db.onUpdate("INSERT INTO problem.problems", 1);

    UUID id = repo.create(ACTOR, content(List.of(), List.of(), List.of(), null));

    assertThat(id).isNotNull();
    var call = db.last("INSERT INTO problem.problems");
    assertThat(call.param("actor")).isEqualTo(ACTOR);
    assertThat(call.param("time")).isEqualTo(1000);
    assertThat(db.count("INSERT INTO problem.problems")).isEqualTo(1);
  }

  @Test
  void createRetriesCodeCollisionsThenGivesUpAfterTwentyAttempts() {
    db.onUpdate("INSERT INTO problem.problems", 0);

    assertThatThrownBy(() -> repo.create(ACTOR, content(List.of(), List.of(), List.of(), null)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("exhausted");
    assertThat(db.count("INSERT INTO problem.problems")).isEqualTo(20);
  }

  @Test
  void writeAiContextReturnsNewVersionTimestamp() {
    Instant stamp = Instant.parse("2026-03-04T05:06:07Z");
    db.onQuery("SET ai_brief", row("updated_at", stamp));

    assertThat(repo.writeAiContext(PROBLEM, ACTOR, "brief", true, false)).isEqualTo(stamp);
    var call = db.last("SET ai_brief");
    assertThat(call.param("noFull")).isEqualTo(true);
    assertThat(call.param("questionOnly")).isEqualTo(false);
  }

  @Test
  void markDuplicatedFromBindsSource() {
    UUID source = UUID.randomUUID();

    repo.markDuplicatedFrom(PROBLEM, source);

    assertThat(db.last("duplicated_from_problem_id").param("source")).isEqualTo(source);
  }

  @Test
  void writeUpdatesRowReplacesRelationsAndRereadsSnapshot() {
    db.onQuery(
        "FOR UPDATE",
        row(
            "code",
            "C",
            "author_id",
            ACTOR,
            "status",
            "DRAFT",
            "title",
            "Two Sum",
            "statement_md",
            "s",
            "constraints_md",
            "c",
            "level_id",
            LEVEL,
            "time_limit_ms",
            1000,
            "memory_limit_mb",
            256,
            "max_output_size_kb",
            64,
            "max_submissions_per_hour",
            null,
            "updated_at",
            Instant.EPOCH));

    var snap = repo.write(PROBLEM, ACTOR, content(List.of(), List.of(), List.of(), null));

    assertThat(snap.content().limits().maxSubmissionsPerHour()).isNull();
    assertThat(db.sqls().get(0)).startsWith("UPDATE problem.problems SET title");
    assertThat(db.sqls().get(db.sqls().size() - 5)).contains("FOR UPDATE");
    assertThat(db.last("SET title").param("quota")).isNull();
  }

  @Test
  void writeFailsWhenTheRowVanishes() {
    assertThatThrownBy(
            () -> repo.write(PROBLEM, ACTOR, content(List.of(), List.of(), List.of(), null)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("unavailable");
  }
}
