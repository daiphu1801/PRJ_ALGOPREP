package com.algoprep.interviewbank.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.common.exception.ResourceConflictException;
import com.algoprep.interviewbank.domain.model.CatalogItem;
import com.algoprep.interviewbank.domain.model.CatalogKind;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Real PostgreSQL; every SQL fragment is chosen by CatalogKind, so each test runs both kinds. */
class JdbcQuestionCatalogRepositoryTest extends PostgresTestBase {
  private final JdbcQuestionCatalogRepository repository =
      new JdbcQuestionCatalogRepository(provider(), ids);

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void listReturnsSeedOrderedBySortOrder(CatalogKind kind) {
    List<CatalogItem> items = repository.list(kind, false);
    assertThat(items).hasSize(kind == CatalogKind.TOPIC ? 5 : 3);
    assertThat(items).extracting(CatalogItem::sortOrder).isSorted();
    assertThat(items).allSatisfy(item -> assertThat(item.questionCount()).isNull());
  }

  @Test
  void starFlagExistsForTopicsOnly() {
    assertThat(repository.list(CatalogKind.TOPIC, false))
        .filteredOn(CatalogItem::usesStarFramework)
        .extracting(CatalogItem::code)
        .containsExactly("BEHAVIORAL");
    assertThat(repository.list(CatalogKind.LEVEL, false)).noneMatch(CatalogItem::usesStarFramework);
  }

  @Test
  void countsComeFromTheMatchingReferenceColumn() {
    question(TOPIC_CS, LEVEL_EASY, "q1", "c");
    question(TOPIC_CS, LEVEL_MEDIUM, "q2", "c");
    retired(TOPIC_CS, LEVEL_MEDIUM, "q3");
    var topics = repository.list(CatalogKind.TOPIC, true);
    assertThat(topics.stream().filter(i -> i.id().equals(TOPIC_CS)).findFirst().orElseThrow())
        .extracting(CatalogItem::questionCount)
        .isEqualTo(3L);
    var levels = repository.list(CatalogKind.LEVEL, true);
    assertThat(levels.stream().filter(i -> i.id().equals(LEVEL_MEDIUM)).findFirst().orElseThrow())
        .extracting(CatalogItem::questionCount)
        .isEqualTo(2L);
  }

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void findReturnsItemOrEmpty(CatalogKind kind) {
    UUID id = kind == CatalogKind.TOPIC ? TOPIC_CS : LEVEL_EASY;
    assertThat(repository.find(kind, id)).isPresent();
    assertThat(repository.find(kind, UUID.randomUUID())).isEmpty();
  }

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void lockRunsWithoutErrorInsideATransaction(CatalogKind kind) {
    // The lock is transaction-scoped; autocommit makes it release immediately, which is enough to
    // prove the statements are valid for both kinds.
    repository.lock(kind);
    assertThat(repository.list(kind, false)).isNotEmpty();
  }

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void nameTakenIsCaseInsensitiveAndHonoursExclusion(CatalogKind kind) {
    UUID id = kind == CatalogKind.TOPIC ? TOPIC_CS : LEVEL_EASY;
    String name = repository.find(kind, id).orElseThrow().displayName();
    assertThat(repository.nameTaken(kind, name.toUpperCase(), null)).isTrue();
    assertThat(repository.nameTaken(kind, name, id)).isFalse();
    assertThat(repository.nameTaken(kind, "No such name", null)).isFalse();
  }

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void createAppendsAtTheEndWithDerivedSlug(CatalogKind kind) {
    CatalogItem created = repository.create(kind, "Cơ sở dữ liệu");
    assertThat(created.code()).isEqualTo("CO_SO_DU_LIEU");
    assertThat(created.sortOrder()).isEqualTo(kind == CatalogKind.TOPIC ? 6 : 4);
    assertThat(created.usesStarFramework()).isFalse();
    assertThat(repository.find(kind, created.id())).isPresent();
  }

  @Test
  void createRetriesWithNumericSuffixWhenSlugIsTaken() {
    var first = repository.create(CatalogKind.LEVEL, "Expert");
    // Different display name (differs by punctuation) but the same slug forces the second attempt.
    var second = repository.create(CatalogKind.LEVEL, "Expert!");
    assertThat(first.code()).isEqualTo("EXPERT");
    assertThat(second.code()).isEqualTo("EXPERT_2");
  }

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void createWithDuplicateDisplayNameMapsToConflictCode(CatalogKind kind) {
    String existing = repository.list(kind, false).get(0).displayName();
    assertThatThrownBy(() -> repository.create(kind, existing.toUpperCase()))
        .isInstanceOfSatisfying(
            ResourceConflictException.class,
            e ->
                assertThat(e.errorCode())
                    .isEqualTo(kind == CatalogKind.LEVEL ? "INB-021" : "INB-020"));
  }

  @Test
  void updateRenamesAndTogglesStarForTopics() {
    repository.update(CatalogKind.TOPIC, TOPIC_CS, "Renamed", true);
    var topic = repository.find(CatalogKind.TOPIC, TOPIC_CS).orElseThrow();
    assertThat(topic.displayName()).isEqualTo("Renamed");
    assertThat(topic.usesStarFramework()).isTrue();
    // Null fields keep the stored value (PATCH semantics).
    repository.update(CatalogKind.TOPIC, TOPIC_CS, null, null);
    assertThat(repository.find(CatalogKind.TOPIC, TOPIC_CS).orElseThrow())
        .extracting(CatalogItem::displayName, CatalogItem::usesStarFramework)
        .containsExactly("Renamed", true);
  }

  @Test
  void updateRenamesLevelsAndIgnoresStar() {
    repository.update(CatalogKind.LEVEL, LEVEL_EASY, "Very easy", true);
    assertThat(repository.find(CatalogKind.LEVEL, LEVEL_EASY).orElseThrow())
        .extracting(CatalogItem::displayName, CatalogItem::usesStarFramework)
        .containsExactly("Very easy", false);
  }

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void updateToTakenNameMapsToConflictCode(CatalogKind kind) {
    var items = repository.list(kind, false);
    UUID first = items.get(0).id();
    String other = items.get(1).displayName();
    assertThatThrownBy(() -> repository.update(kind, first, other, null))
        .isInstanceOfSatisfying(
            ResourceConflictException.class,
            e ->
                assertThat(e.errorCode())
                    .isEqualTo(kind == CatalogKind.LEVEL ? "INB-021" : "INB-020"));
  }

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void reorderRewritesSortOrderFromListPosition(CatalogKind kind) {
    var reversed = repository.list(kind, false).stream().map(CatalogItem::id).toList().reversed();
    repository.reorder(kind, reversed);
    assertThat(repository.list(kind, false).stream().map(CatalogItem::id).toList())
        .isEqualTo(reversed);
  }

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void referencesCountsEveryStatusAndDeleteRemovesTheRow(CatalogKind kind) {
    UUID used = kind == CatalogKind.TOPIC ? TOPIC_CS : LEVEL_EASY;
    question(TOPIC_CS, LEVEL_EASY, "a", "c");
    retired(TOPIC_CS, LEVEL_EASY, "b");
    assertThat(repository.references(kind, used)).isEqualTo(2L);
    UUID free = repository.create(kind, "Brand new").id();
    assertThat(repository.references(kind, free)).isZero();
    repository.delete(kind, free);
    assertThat(repository.find(kind, free)).isEmpty();
  }
}
