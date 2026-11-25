package com.algoprep.problembank.infrastructure.persistence;

import static com.algoprep.problembank.infrastructure.persistence.FakeJdbc.row;
import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.problembank.domain.model.assignment.ClassAssignmentModels.AssignableFilter;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Class assignment persistence (DD F2 sections 50 to 55). */
class JdbcClassAssignmentRepositoryTest {
  private static final UUID P = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID C = UUID.fromString("00000000-0000-0000-0000-000000000002");
  private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000003");

  private final FakeJdbc db = new FakeJdbc();
  private final JdbcClassAssignmentRepository repo =
      new JdbcClassAssignmentRepository(db.provider());

  @Test
  void activeMapsRowsAndBindsClassLiteralAndEscapedPattern() {
    db.onQuery(
        "FROM problem.class_assignments ca",
        row(
            "problem_id",
            P,
            "class_id",
            C,
            "assigned_at",
            Instant.parse("2026-04-01T00:00:00Z"),
            "title",
            "Two Sum",
            "topic_names",
            new Object[] {"Array"},
            "level_code",
            "EASY",
            "display_name",
            "Easy",
            "withdrawn",
            true));

    var rows = repo.active(Set.of(C), "50%_x");

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).withdrawn()).isTrue();
    assertThat(rows.get(0).topicNames()).containsExactly("Array");
    assertThat(rows.get(0).assignedAt()).isEqualTo(Instant.parse("2026-04-01T00:00:00Z"));
    var call = db.last("FROM problem.class_assignments ca");
    assertThat(call.param("classes")).isEqualTo("{" + C + "}");
    assertThat(call.param("pattern")).isEqualTo("%50\\%\\_x%");
  }

  @Test
  void activeWithNoSearchBindsNullPatternAndEmptyClassSetIsEmptyLiteral() {
    var rows = repo.active(Set.of(), null);

    assertThat(rows).isEmpty();
    var call = db.last("FROM problem.class_assignments ca");
    assertThat(call.param("classes")).isEqualTo("{}");
    assertThat(call.param("pattern")).isNull();
  }

  @Test
  void assignablePagesAndClampsPage() {
    db.onQuery("SELECT count(*)", 25L)
        .onQuery(
            "SELECT p.id, p.code",
            row(
                "id", P,
                "code", "P1",
                "title", "T",
                "level_code", "EASY",
                "display_name", "Easy",
                "topic_names", new Object[] {},
                "assigned", true));

    var page = repo.assignable(C, new AssignableFilter(null, null, null), 99, 10);

    assertThat(page.totalItems()).isEqualTo(25);
    assertThat(page.page()).isEqualTo(3);
    assertThat(page.items()).singleElement().extracting("code").isEqualTo("P1");
    var call = db.last("SELECT p.id, p.code");
    assertThat(call.param("offset")).isEqualTo(20L);
    assertThat(call.param("limit")).isEqualTo(10);
    assertThat(call.param("class")).isEqualTo(C);
  }

  @Test
  void assignableOfEmptyResultStaysOnPageOneWithEscapedSearch() {
    db.onQuery("SELECT count(*)", 0L);

    var page = repo.assignable(C, new AssignableFilter("a_b", null, null), 4, 10);

    assertThat(page.page()).isEqualTo(1);
    assertThat(page.items()).isEmpty();
    assertThat(db.last("SELECT p.id, p.code").param("pattern")).isEqualTo("%a\\_b%");
  }

  @Test
  void problemStateDistinguishesMissingFromWithdrawn() {
    assertThat(repo.problemState(P)).isEmpty();
    db.onQuery("(status = 'PUBLISHED' AND NOT deleted)", row("title", "T", "published", false));

    var state = repo.problemState(P).orElseThrow();

    assertThat(state.title()).isEqualTo("T");
    assertThat(state.published()).isFalse();
  }

  @Test
  void insertActiveReportsWhetherARowWasCreated() {
    assertThat(repo.insertActive(P, C, ACTOR)).isFalse();
    db.onUpdate("INSERT INTO problem.class_assignments", 1);
    assertThat(repo.insertActive(P, C, ACTOR)).isTrue();
    assertThat(db.last("INSERT INTO problem.class_assignments").param("actor")).isEqualTo(ACTOR);
  }

  @Test
  void deactivateReturnsRemovalTimeOrEmptyWhenNothingActive() {
    assertThat(repo.deactivate(C, P)).isEmpty();
    Instant stamp = Instant.parse("2026-04-02T00:00:00Z");
    db.onQuery("RETURNING removed_at", row("removed_at", stamp));

    assertThat(repo.deactivate(C, P)).contains(stamp);
  }
}
