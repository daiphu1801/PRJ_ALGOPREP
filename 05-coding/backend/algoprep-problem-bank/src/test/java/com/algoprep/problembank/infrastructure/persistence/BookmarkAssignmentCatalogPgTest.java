package com.algoprep.problembank.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.common.exception.ResourceConflictException;
import com.algoprep.problembank.domain.model.catalog.CatalogItem;
import com.algoprep.problembank.domain.model.catalog.CatalogKind;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

/** Bookmarks, class assignments and catalogue CRUD against real constraints and indexes. */
class BookmarkAssignmentCatalogPgTest extends PostgresTestBase {
  private static final UUID USER = id(800);
  private static final UUID CLASS_A = id(500);
  private static final UUID CLASS_B = id(501);

  private JdbcBookmarkRepository bookmarks;
  private JdbcClassAssignmentRepository assignments;
  private JdbcClassAssignmentReadRepository assignmentRead;
  private JdbcStudentClassAssignmentReadRepository studentAssignments;
  private JdbcClassDeletedRepository classDeleted;
  private JdbcCatalogRepository catalogue;
  private UUID p1;
  private UUID p2;

  @BeforeEach
  void seed() {
    bookmarks = new JdbcBookmarkRepository(provider());
    assignments = new JdbcClassAssignmentRepository(provider());
    assignmentRead = new JdbcClassAssignmentReadRepository(provider());
    studentAssignments = new JdbcStudentClassAssignmentReadRepository(provider());
    classDeleted = new JdbcClassDeletedRepository(provider());
    catalogue = new JdbcCatalogRepository(provider(), UUID::randomUUID);
    p1 = published(1, "P-ONE");
    p2 = published(2, "P-TWO");
  }

  // ---- bookmarks ------------------------------------------------------------------------------

  @Test
  void bookmarkInsertIsIdempotentAndReportsCreationOnlyOnce() {
    var first = bookmarks.insert(USER, p1, "first");
    var second = bookmarks.insert(USER, p1, "second");

    assertThat(first.created()).isTrue();
    assertThat(second.created()).isFalse();
    assertThat(second.note()).isEqualTo("second");
    assertThat(second.savedAt()).isEqualTo(first.savedAt());
    assertThat(count("bookmarks")).isEqualTo(1);
    assertThat(jdbc.sql("SELECT note FROM problem.bookmarks").query(String.class).single())
        .isEqualTo("second");
  }

  @Test
  void bookmarkOfUnknownProblemViolatesTheForeignKey() {
    assertThatThrownBy(() -> bookmarks.insert(USER, id(999), null))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(count("bookmarks")).isZero();
  }

  @Test
  void updateNoteTouchesOnlyAnExistingRow() {
    assertThat(bookmarks.updateNote(USER, p1, "x")).isEmpty();
    bookmarks.insert(USER, p1, "old");

    var saved = bookmarks.updateNote(USER, p1, "new").orElseThrow();

    assertThat(saved.note()).isEqualTo("new");
    assertThat(saved.created()).isFalse();
    assertThat(bookmarks.exists(USER, p1)).isTrue();
    assertThat(bookmarks.exists(id(801), p1)).isFalse();
    assertThat(count("bookmarks")).isEqualTo(1);
  }

  @Test
  void bookmarkListIsNewestFirstAndFlagsWithdrawnProblems() {
    link(p1, MANG);
    bookmarks.insert(USER, p1, "a");
    bookmarks.insert(USER, p2, null);
    bookmarks.insert(id(801), p2, "other user");
    jdbc.sql("UPDATE problem.bookmarks SET created_at=:t WHERE problem_id=:p AND user_id=:u")
        .param("t", utc(Instant.now().minus(1, ChronoUnit.DAYS)))
        .param("p", p1)
        .param("u", USER)
        .update();
    jdbc.sql("UPDATE problem.problems SET status='UNPUBLISHED' WHERE id=:id")
        .param("id", p1)
        .update();

    var list = bookmarks.list(USER);

    assertThat(list).extracting(b -> b.problemId()).containsExactly(p2, p1);
    assertThat(list.get(0).withdrawn()).isFalse();
    assertThat(list.get(1).withdrawn()).isTrue();
    assertThat(list.get(1).topicNames()).containsExactly("Mảng");
    assertThat(bookmarks.problemPublished(p1)).isFalse();
    assertThat(bookmarks.problemPublished(p2)).isTrue();
    assertThat(bookmarks.levelCodeExists("EASY")).isTrue();
    assertThat(bookmarks.levelCodeExists("NOPE")).isFalse();
    bookmarks.delete(USER, p1);
    assertThat(count("bookmarks", "user_id = '" + USER + "'")).isEqualTo(1);
  }

  // ---- class assignments -----------------------------------------------------------------------

  @Test
  void onlyOneActiveAssignmentPerProblemAndClassButReassignAfterRemovalWorks() {
    assertThat(assignments.insertActive(p1, CLASS_A, AUTHOR)).isTrue();
    assertThat(assignments.insertActive(p1, CLASS_A, AUTHOR)).isFalse();
    assertThat(count("class_assignments")).isEqualTo(1);

    assertThat(assignments.deactivate(CLASS_A, p1)).isPresent();
    assertThat(assignments.deactivate(CLASS_A, p1)).isEmpty();
    assertThat(assignments.insertActive(p1, CLASS_A, AUTHOR)).isTrue();

    assertThat(count("class_assignments")).isEqualTo(2);
    assertThat(count("class_assignments", "removed_at IS NULL")).isEqualTo(1);
  }

  @Test
  void rawDuplicateActiveInsertHitsThePartialUniqueIndex() {
    assignment(p1, CLASS_A, false);

    assertThatThrownBy(() -> assignment(p1, CLASS_A, false))
        .isInstanceOf(DuplicateKeyException.class);
    assignment(p1, CLASS_A, true);
    assertThat(count("class_assignments")).isEqualTo(2);
  }

  @Test
  void assignmentOfUnknownProblemViolatesTheForeignKey() {
    assertThatThrownBy(() -> assignments.insertActive(id(999), CLASS_A, AUTHOR))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void activeListShowsTitlesFiltersByTitleAndFlagsWithdrawn() {
    assignment(p1, CLASS_A, false);
    assignment(p2, CLASS_A, false);
    assignment(p2, CLASS_B, false);
    assignment(p1, CLASS_B, true);
    jdbc.sql("UPDATE problem.problems SET title='Odd 100% title' WHERE id=:id")
        .param("id", p2)
        .update();
    softDelete(p1);

    var all = assignments.active(Set.of(CLASS_A), null);
    var byTitle = assignments.active(Set.of(CLASS_A, CLASS_B), "100%");

    assertThat(all).hasSize(2);
    assertThat(all)
        .filteredOn(r -> r.problemId().equals(p1))
        .singleElement()
        .satisfies(r -> assertThat(r.withdrawn()).isTrue());
    assertThat(byTitle).extracting(r -> r.problemId()).containsOnly(p2);
    assertThat(byTitle).hasSize(2);
    assertThat(assignments.active(Set.of(CLASS_B), null)).hasSize(1);
  }

  @Test
  void assignableExcludesAssignedAndUnpublishedAndPages() {
    for (int i = 10; i < 15; i++) {
      published(i, "BULK-" + i);
    }
    problem(30, "DRAFT", "Draft", "UNPUBLISHED", EASY);
    assignment(p1, CLASS_A, false);
    assignment(p2, CLASS_A, true);

    var page = assignments.assignable(CLASS_A, null, 9, 3);
    var search = assignments.assignable(CLASS_A, "bulk-1", 1, 10);

    assertThat(page.totalItems()).isEqualTo(6);
    assertThat(page.page()).isEqualTo(2);
    assertThat(page.items()).hasSize(3);
    assertThat(search.totalItems()).isEqualTo(5);
    assertThat(assignments.assignable(CLASS_B, null, 1, 50).totalItems()).isEqualTo(7);
    assertThat(assignments.problemState(p1).orElseThrow().published()).isTrue();
    assertThat(assignments.problemState(id(999))).isEmpty();
  }

  @Test
  void readSidesSeeRemovedRowsOrOnlyActiveOnesAsContracted() {
    assignment(p1, CLASS_A, false);
    assignment(p2, CLASS_A, true);
    softDelete(p2);

    var snapshots = assignmentRead.findAssignments(Set.of(CLASS_A));
    var published = studentAssignments.active(Set.of(CLASS_A), true);
    var all = studentAssignments.active(Set.of(CLASS_A), false);

    assertThat(snapshots).hasSize(2);
    assertThat(snapshots).filteredOn(s -> s.removedAt() != null).hasSize(1);
    assertThat(published).extracting(a -> a.problemId()).containsExactly(p1);
    assertThat(all).extracting(a -> a.problemId()).containsExactly(p1);
    assertThat(assignmentRead.findAssignments(Set.of())).isEmpty();
    assertThat(studentAssignments.active(Set.of(), true)).isEmpty();
  }

  @Test
  void classDeletedClosesEveryActiveAssignmentOfThatClassOnly() {
    assignment(p1, CLASS_A, false);
    assignment(p2, CLASS_A, false);
    assignment(p1, CLASS_B, false);

    assertThat(classDeleted.removeAssignments(CLASS_A)).isEqualTo(2);
    assertThat(classDeleted.removeAssignments(CLASS_A)).isZero();

    assertThat(count("class_assignments", "removed_at IS NULL")).isEqualTo(1);
    assertThat(count("class_assignments", "class_id = '" + CLASS_B + "' AND removed_at IS NULL"))
        .isEqualTo(1);
  }

  // ---- catalogue -------------------------------------------------------------------------------

  private static String table(CatalogKind kind) {
    return kind == CatalogKind.LEVEL ? "problem_levels" : "topics";
  }

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void listCountsReferencesPerKind(CatalogKind kind) {
    link(p1, MANG);
    link(p2, MANG);

    List<CatalogItem> plain = catalogue.list(kind, false);
    List<CatalogItem> counted = catalogue.list(kind, true);

    assertThat(plain).extracting(CatalogItem::problemCount).containsOnlyNulls();
    assertThat(counted.get(0).problemCount()).isEqualTo(2L);
    assertThat(counted).extracting(CatalogItem::sortOrder).isSorted();
  }

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void createAppendsASlugAndRejectsTheCaseInsensitiveDuplicateName(CatalogKind kind) {
    var created = catalogue.create(kind, "Đồ án mới");

    assertThat(created.code()).isEqualTo("DO-AN-MOI");
    assertThat(created.sortOrder()).isEqualTo(kind == CatalogKind.LEVEL ? 4 : 7);
    assertThat(count(table(kind), "code = 'DO-AN-MOI'")).isEqualTo(1);

    String existing = kind == CatalogKind.LEVEL ? "DỄ" : "mảng";
    int before = count(table(kind));
    assertThatThrownBy(() -> catalogue.create(kind, existing))
        .isInstanceOf(ResourceConflictException.class)
        .hasMessageContaining("already exists");
    assertThat(count(table(kind))).isEqualTo(before);
  }

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void sameSlugDifferentNameGetsANumericSuffix(CatalogKind kind) {
    var first = catalogue.create(kind, "A b");
    var second = catalogue.create(kind, "A-b");

    assertThat(first.code()).isEqualTo("A-B");
    assertThat(second.code()).isEqualTo("A-B-2");
  }

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void renameReorderReferencesAndDelete(CatalogKind kind) {
    var created = catalogue.create(kind, "Temporary");
    UUID seeded = kind == CatalogKind.LEVEL ? EASY : MANG;
    String column = kind == CatalogKind.LEVEL ? "display_name" : "name";

    catalogue.rename(kind, created.id(), "Renamed");
    assertThat(
            jdbc.sql("SELECT " + column + " FROM problem." + table(kind) + " WHERE id=:id")
                .param("id", created.id())
                .query(String.class)
                .single())
        .isEqualTo("Renamed");
    String clash = kind == CatalogKind.LEVEL ? "trung bình" : "chuỗi";
    assertThatThrownBy(() -> catalogue.rename(kind, created.id(), clash))
        .isInstanceOf(ResourceConflictException.class);

    catalogue.reorder(kind, List.of(created.id(), seeded));
    assertThat(catalogue.list(kind, false).get(0).id()).isEqualTo(created.id());

    link(p1, MANG);
    assertThat(catalogue.references(CatalogKind.TOPIC, MANG)).isEqualTo(1);
    assertThat(catalogue.references(CatalogKind.LEVEL, EASY)).isEqualTo(2);
    assertThat(catalogue.references(kind, created.id())).isZero();
    catalogue.delete(kind, created.id());
    assertThat(count(table(kind), "id = '" + created.id() + "'")).isZero();
  }

  @Test
  void deletingAReferencedLevelOrTopicViolatesTheForeignKey() {
    link(p1, MANG);

    assertThatThrownBy(() -> catalogue.delete(CatalogKind.LEVEL, EASY))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> catalogue.delete(CatalogKind.TOPIC, MANG))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(count("problem_levels")).isEqualTo(3);
    assertThat(count("topics")).isEqualTo(6);
  }

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void lockTakesTheKindAdvisoryLockInsideTheTransaction(CatalogKind kind) {
    long key = kind == CatalogKind.LEVEL ? 2201001L : 2201002L;

    long held =
        tx.execute(
            status -> {
              catalogue.lock(kind);
              return jdbc.sql(
                      "SELECT count(*) FROM pg_locks WHERE locktype='advisory'"
                          + " AND objid=:key AND pid=pg_backend_pid() AND granted")
                  .param("key", key)
                  .query(Long.class)
                  .single();
            });

    assertThat(held).isEqualTo(1);
  }
}
