package com.algoprep.problembank.infrastructure.persistence;

import static com.algoprep.problembank.infrastructure.persistence.FakeJdbc.row;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Small read-side repositories: harness spec, suggestions, class assignment reads, history. */
class JdbcReadOnlyRepositoriesTest {
  private static final UUID P = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID C = UUID.fromString("00000000-0000-0000-0000-000000000002");
  private static final UUID T = UUID.fromString("00000000-0000-0000-0000-000000000003");
  private static final Instant NOW = Instant.parse("2026-10-01T00:00:00Z");

  private final FakeJdbc db = new FakeJdbc();

  @Test
  void harnessSpecMapsSpecAndWrapperFlag() {
    var repo = new JdbcHarnessProblemReadRepository(db.provider());
    assertThat(repo.findSpec(P)).isEmpty();
    db.onQuery(
        "SELECT s.*, p.function_wrapper_supported",
        row(
            "spec_version", 4,
            "matching_strategy", "EXACT",
            "epsilon_value", new BigDecimal("1"),
            "stdin_format_md", "a",
            "stdout_format_md", "b",
            "function_name", "f",
            "return_type", "{}",
            "parameters", "[]",
            "name_overrides", null,
            "function_wrapper_supported", true));

    var view = repo.findSpec(P).orElseThrow();

    assertThat(view.specVersion()).isEqualTo(4);
    assertThat(view.functionWrapperSupported()).isTrue();
    assertThat(view.data().functionName()).isEqualTo("f");
    assertThat(view.problemId()).isEqualTo(P);
  }

  @Test
  void canReadBindsAdminFlagOnlyForAdminCategory() {
    var repo = new JdbcHarnessProblemReadRepository(db.provider());
    db.onQuery("SELECT EXISTS", true);

    assertThat(repo.canRead(P, C, "ADMIN")).isTrue();
    assertThat(db.last("SELECT EXISTS").param("admin")).isEqualTo(true);
    repo.canRead(P, C, "INSTRUCTOR");
    assertThat(db.last("SELECT EXISTS").param("admin")).isEqualTo(false);
    assertThat(db.last("SELECT EXISTS").param("caller")).isEqualTo(C);
    repo.canRead(P, C, null);
    assertThat(db.last("SELECT EXISTS").param("admin")).isEqualTo(false);
  }

  private Map<String, Object> candidate(Object topicIds) {
    return row(
        "id",
        P,
        "code",
        "P1",
        "title",
        "T",
        "level_code",
        "EASY",
        "display_name",
        "Easy",
        "ac_rate",
        null,
        "function_wrapper_supported",
        false,
        "topic_names",
        new Object[] {"Array"},
        "topic_ids",
        topicIds);
  }

  @Test
  void suggestedCandidatesParseTopicIdsAndBindFilters() {
    var repo = new JdbcSuggestedProblemReadRepository(db.provider());
    db.onQuery("AS topic_ids", candidate(new Object[] {T.toString()}));

    var found = repo.candidates("EASY", T);

    assertThat(found)
        .singleElement()
        .satisfies(
            c -> {
              assertThat(c.topicIds()).containsExactly(T);
              assertThat(c.acRate()).isNull();
              assertThat(c.functionWrapperSupported()).isFalse();
            });
    var call = db.last("AS topic_ids");
    assertThat(call.param("difficulty")).isEqualTo("EASY");
    assertThat(call.param("topic")).isEqualTo(T.toString());
  }

  @Test
  void suggestedCandidatesWithoutFiltersBindNullsAndAllowEmptyTopics() {
    var repo = new JdbcSuggestedProblemReadRepository(db.provider());
    db.onQuery("AS topic_ids", candidate(new Object[] {}));

    var found = repo.candidates(null, null);

    assertThat(found.get(0).topicIds()).isEmpty();
    assertThat(db.last("AS topic_ids").param("topic")).isNull();
    assertThat(db.last("AS topic_ids").param("difficulty")).isNull();
  }

  @Test
  void studentAssignmentsReturnEmptyWithoutQueryForNoClasses() {
    var repo = new JdbcStudentClassAssignmentReadRepository(db.provider());

    assertThat(repo.active(Set.of(), true)).isEmpty();
    assertThat(db.calls()).isEmpty();
  }

  @Test
  void studentAssignmentsMapRowsAndPassPublishedOnlyFlag() {
    var repo = new JdbcStudentClassAssignmentReadRepository(db.provider());
    db.onQuery(
        "FROM problem.class_assignments a",
        row(
            "class_id",
            C,
            "problem_id",
            P,
            "code",
            "P1",
            "title",
            "T",
            "display_name",
            "Easy",
            "assigned_at",
            NOW,
            "withdrawn",
            true));

    var rows = repo.active(Set.of(C), false);

    assertThat(rows)
        .singleElement()
        .satisfies(
            a -> {
              assertThat(a.withdrawn()).isTrue();
              assertThat(a.assignedAt()).isEqualTo(NOW);
              assertThat(a.classId()).isEqualTo(C);
            });
    assertThat(db.last("FROM problem.class_assignments a").param("publishedOnly")).isEqualTo(false);
  }

  @Test
  void assignmentHistoryKeepsRemovedRowsAndMapsNullRemovalTime() {
    var repo = new JdbcClassAssignmentReadRepository(db.provider());
    assertThat(repo.findAssignments(Set.of())).isEmpty();
    assertThat(db.calls()).isEmpty();
    db.onQuery(
        "FROM problem.class_assignments a",
        row("class_id", C, "problem_id", P, "title", "T", "assigned_at", NOW, "removed_at", null),
        row(
            "class_id", C,
            "problem_id", P,
            "title", "T",
            "assigned_at", NOW,
            "removed_at", NOW.plusSeconds(60)));

    var rows = repo.findAssignments(Set.of(C));

    assertThat(rows.get(0).removedAt()).isNull();
    assertThat(rows.get(1).removedAt()).isEqualTo(NOW.plusSeconds(60));
  }

  @Test
  void historicalTopicProblemsGroupByTopicAndKeepEmptyTopics() {
    var repo = new JdbcHistoricalTopicProblemRepository(db.provider());
    UUID emptyTopic = UUID.randomUUID();
    UUID p2 = UUID.randomUUID();
    db.onQuery(
        "FROM problem.problem_topics",
        row("topic_id", T, "problem_id", P),
        row("topic_id", T, "problem_id", p2));

    var result = repo.read(Set.of(T, emptyTopic));

    assertThat(result.get(T)).containsExactlyInAnyOrder(P, p2);
    assertThat(result.get(emptyTopic)).isEmpty();
  }

  @Test
  void historicalTopicProblemsOfNoTopicsIsEmptyWithoutQuery() {
    var repo = new JdbcHistoricalTopicProblemRepository(db.provider());

    assertThat(repo.read(Set.of())).isEmpty();
    assertThat(db.calls()).isEmpty();
  }

  @Test
  void classDeletedRemovesOnlyActiveAssignmentsAndReturnsAffectedCount() {
    var repo = new JdbcClassDeletedRepository(db.provider());
    db.onUpdate("UPDATE problem.class_assignments SET removed_at", 3);

    assertThat(repo.removeAssignments(C)).isEqualTo(3);
    assertThat(db.last("SET removed_at").sql()).contains("removed_at IS NULL");
    assertThat(db.last("SET removed_at").param("classId")).isEqualTo(C);
  }
}
