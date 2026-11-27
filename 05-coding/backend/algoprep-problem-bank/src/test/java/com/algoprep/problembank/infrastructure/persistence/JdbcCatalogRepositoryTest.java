package com.algoprep.problembank.infrastructure.persistence;

import static com.algoprep.problembank.infrastructure.persistence.FakeJdbc.row;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.common.exception.ResourceConflictException;
import com.algoprep.problembank.domain.model.catalog.CatalogKind;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

/** Level and topic catalogue persistence: identifiers come from CatalogKind only. */
class JdbcCatalogRepositoryTest {
  private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  private final FakeJdbc db = new FakeJdbc();
  private final JdbcCatalogRepository repo = new JdbcCatalogRepository(db.provider(), () -> ID);

  private static Object item(int order, Object count) {
    return row(
        "id",
        ID,
        "code",
        "c" + order,
        "name",
        "N" + order,
        "sort_order",
        order,
        "problem_count",
        count);
  }

  @Test
  void listWithoutCountsSelectsNullCountColumn() {
    db.onQuery("FROM problem.problem_levels c", item(1, null));

    var items = repo.list(CatalogKind.LEVEL, false);

    assertThat(items).singleElement().satisfies(i -> assertThat(i.problemCount()).isNull());
    assertThat(db.sqls().get(0)).contains("c.display_name AS name").contains("NULL::bigint");
  }

  @Test
  void listWithCountsUsesTheRightReferenceTablePerKind() {
    db.onQuery("FROM problem.problem_levels c", item(1, 4L))
        .onQuery("FROM problem.topics c", item(2, 9L));

    var levels = repo.list(CatalogKind.LEVEL, true);
    var topics = repo.list(CatalogKind.TOPIC, true);

    assertThat(levels.get(0).problemCount()).isEqualTo(4L);
    assertThat(topics.get(0).problemCount()).isEqualTo(9L);
    assertThat(db.sqls().get(0)).contains("p.level_id = c.id");
    assertThat(db.sqls().get(1)).contains("p.topic_id = c.id").contains("c.name AS name");
  }

  @Test
  void lockTakesDistinctAdvisoryKeyPerKindThenLocksRows() {
    db.onQuery("pg_advisory_xact_lock", row("v", 1));

    repo.lock(CatalogKind.LEVEL);
    repo.lock(CatalogKind.TOPIC);

    assertThat(db.calls().get(0).param("key")).isEqualTo(2201001L);
    assertThat(db.calls().get(2).param("key")).isEqualTo(2201002L);
    assertThat(db.calls().get(1).sql()).contains("problem.problem_levels").endsWith("FOR UPDATE");
    assertThat(db.calls().get(3).sql()).contains("problem.topics");
  }

  @Test
  void createAppendsAfterHighestSortOrder() {
    db.onQuery("FROM problem.problem_levels c", item(1, null), item(7, null))
        .onUpdate("INSERT INTO problem.problem_levels", 1);

    var created = repo.create(CatalogKind.LEVEL, "Medium");

    assertThat(created.sortOrder()).isEqualTo(8);
    assertThat(created.name()).isEqualTo("Medium");
    assertThat(created.id()).isEqualTo(ID);
    assertThat(db.last("INSERT INTO problem.problem_levels").param("order")).isEqualTo(8);
  }

  @Test
  void createStartsAtOneForEmptyCatalogue() {
    db.onUpdate("INSERT INTO problem.topics", 1);

    assertThat(repo.create(CatalogKind.TOPIC, "Graph").sortOrder()).isEqualTo(1);
  }

  @Test
  void createRetriesOnSlugCollisionAndGivesUpAfterTwentyAttempts() {
    db.onUpdate("INSERT INTO problem.topics", 0);

    assertThatThrownBy(() -> repo.create(CatalogKind.TOPIC, "Graph"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("collision");
    assertThat(db.count("INSERT INTO problem.topics")).isEqualTo(20);
  }

  @Test
  void duplicateKeyMapsToKindSpecificConflictCode() {
    db.onFail("INSERT INTO problem.problem_levels", new DuplicateKeyException("dup"));
    db.onFail("INSERT INTO problem.topics", new DuplicateKeyException("dup"));

    assertThatThrownBy(() -> repo.create(CatalogKind.LEVEL, "Easy"))
        .isInstanceOf(ResourceConflictException.class)
        .hasMessageContaining("already exists");
    assertThatThrownBy(() -> repo.create(CatalogKind.TOPIC, "Array"))
        .isInstanceOf(ResourceConflictException.class);
  }

  @Test
  void renameUsesKindColumnAndMapsDuplicateToConflict() {
    repo.rename(CatalogKind.LEVEL, ID, "Hard");
    repo.rename(CatalogKind.TOPIC, ID, "Trees");
    assertThat(db.sqls().get(0)).contains("SET display_name = :name");
    assertThat(db.sqls().get(1)).contains("problem.topics SET name = :name");

    db.onFail("UPDATE problem.topics SET name", new DuplicateKeyException("dup"));
    assertThatThrownBy(() -> repo.rename(CatalogKind.TOPIC, ID, "Trees"))
        .isInstanceOf(ResourceConflictException.class);
  }

  @Test
  void reorderWritesOneBasedPositionsInGivenOrder() {
    UUID second = UUID.randomUUID();

    repo.reorder(CatalogKind.TOPIC, List.of(ID, second));

    assertThat(db.calls()).hasSize(2);
    assertThat(db.calls().get(0).param("order")).isEqualTo(1);
    assertThat(db.calls().get(1).param("id")).isEqualTo(second);
    assertThat(db.calls().get(1).param("order")).isEqualTo(2);
  }

  @Test
  void referencesCountProblemsForLevelsAndLinksForTopics() {
    db.onQuery("FROM problem.problems WHERE level_id", 3L)
        .onQuery("FROM problem.problem_topics WHERE topic_id", 0L);

    assertThat(repo.references(CatalogKind.LEVEL, ID)).isEqualTo(3L);
    assertThat(repo.references(CatalogKind.TOPIC, ID)).isZero();
  }

  @Test
  void deleteTargetsTheKindTable() {
    repo.delete(CatalogKind.LEVEL, ID);
    repo.delete(CatalogKind.TOPIC, ID);

    assertThat(db.sqls().get(0)).isEqualTo("DELETE FROM problem.problem_levels WHERE id = :id");
    assertThat(db.sqls().get(1)).isEqualTo("DELETE FROM problem.topics WHERE id = :id");
  }
}
