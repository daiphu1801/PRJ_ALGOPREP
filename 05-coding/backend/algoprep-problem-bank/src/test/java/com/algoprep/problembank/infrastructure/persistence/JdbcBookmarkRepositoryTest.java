package com.algoprep.problembank.infrastructure.persistence;

import static com.algoprep.problembank.infrastructure.persistence.FakeJdbc.row;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Bookmark persistence: per-user scoping, upsert flag, withdrawn problems stay listed. */
class JdbcBookmarkRepositoryTest {
  private static final UUID USER = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
  private static final UUID P = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final Instant T = Instant.parse("2026-06-01T00:00:00Z");

  private final FakeJdbc db = new FakeJdbc();
  private final JdbcBookmarkRepository repo = new JdbcBookmarkRepository(db.provider());

  @Test
  void listMapsRowsForTheCallerOnly() {
    db.onQuery(
        "FROM problem.bookmarks b",
        row(
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
            "note",
            null,
            "created_at",
            T,
            "withdrawn",
            true,
            "topic_names",
            new Object[] {"DP"}));

    var items = repo.list(USER);

    assertThat(items)
        .singleElement()
        .satisfies(
            b -> {
              assertThat(b.withdrawn()).isTrue();
              assertThat(b.note()).isNull();
              assertThat(b.savedAt()).isEqualTo(T);
              assertThat(b.topicNames()).containsExactly("DP");
            });
    assertThat(db.last("FROM problem.bookmarks b").param("user")).isEqualTo(USER);
    assertThat(repo.list(UUID.randomUUID())).isNotNull();
  }

  @Test
  void levelCodeExistsAndProblemPublishedAndExistsReadBooleans() {
    db.onQuery("FROM problem.problem_levels WHERE code", true)
        .onQuery("status = 'PUBLISHED'", false)
        .onQuery("FROM problem.bookmarks", true);

    assertThat(repo.levelCodeExists("EASY")).isTrue();
    assertThat(repo.problemPublished(P)).isFalse();
    assertThat(repo.exists(USER, P)).isTrue();
    assertThat(db.last("FROM problem.bookmarks").param("problem")).isEqualTo(P);
  }

  @Test
  void updateNoteIsEmptyWhenNoBookmarkAndNeverReportsCreated() {
    assertThat(repo.updateNote(USER, P, "n")).isEmpty();
    db.onQuery("UPDATE problem.bookmarks SET note", row("note", "n", "created_at", T));

    var saved = repo.updateNote(USER, P, "n").orElseThrow();

    assertThat(saved.created()).isFalse();
    assertThat(saved.note()).isEqualTo("n");
  }

  @Test
  void insertReportsCreatedFlagFromUpsert() {
    db.onQuery("INSERT INTO problem.bookmarks", row("note", "x", "created_at", T, "created", true));

    var saved = repo.insert(USER, P, "x");

    assertThat(saved.created()).isTrue();
    assertThat(saved.savedAt()).isEqualTo(T);
    assertThat(db.last("INSERT INTO problem.bookmarks").param("note")).isEqualTo("x");
  }

  @Test
  void deleteIsScopedByUserAndProblem() {
    repo.delete(USER, P);

    var call = db.last("DELETE FROM problem.bookmarks");
    assertThat(call.param("user")).isEqualTo(USER);
    assertThat(call.param("problem")).isEqualTo(P);
  }
}
