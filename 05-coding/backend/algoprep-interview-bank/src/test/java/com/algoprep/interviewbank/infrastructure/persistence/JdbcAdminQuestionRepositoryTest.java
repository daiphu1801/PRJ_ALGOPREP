package com.algoprep.interviewbank.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.interviewbank.domain.model.AdminQuestion;
import com.algoprep.interviewbank.domain.model.AdminQuestionPage;
import com.algoprep.interviewbank.domain.model.AdminQuestionRow;
import com.algoprep.interviewbank.domain.model.CatalogKind;
import com.algoprep.interviewbank.domain.model.QuestionDraft;
import com.algoprep.interviewbank.domain.model.QuestionListCriteria;
import com.algoprep.interviewbank.domain.model.QuestionSort;
import com.algoprep.interviewbank.domain.model.RubricCriterion;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Real PostgreSQL; covers the management list, locks, writes and the progress recalculation. */
class JdbcAdminQuestionRepositoryTest extends PostgresTestBase {
  private final JdbcAdminQuestionRepository repository =
      new JdbcAdminQuestionRepository(provider(), ids);
  private final UUID actor = UUID.randomUUID();

  private static QuestionDraft draft(String title, List<RubricCriterion> rubric) {
    return new QuestionDraft(
        TOPIC_CS,
        LEVEL_EASY,
        title,
        "body",
        List.of("why?", "how?"),
        rubric,
        "approach",
        List.of("kw"),
        "framework");
  }

  private void answer(UUID user, UUID question, int no, String status, String feedbackJson) {
    jdbc.sql(
            "INSERT INTO interview_bank.user_answers(id, user_id, question_id, answer_text,"
                + " attempt_no, feedback_status, feedback_result_json, rubric_snapshot_json)"
                + " VALUES (:id, :u, :q, 'a', :no, :s, CAST(:fb AS jsonb), '[]'::jsonb)")
        .param("id", UUID.randomUUID())
        .param("u", user)
        .param("q", question)
        .param("no", no)
        .param("s", status)
        .param("fb", feedbackJson, java.sql.Types.VARCHAR)
        .update();
  }

  private static String feedback(int percentA, int weightA, int percentB, int weightB) {
    return "{\"criteria\":[{\"achievementPercent\":"
        + percentA
        + ",\"weightPercent\":"
        + weightA
        + "},{\"achievementPercent\":"
        + percentB
        + ",\"weightPercent\":"
        + weightB
        + "}]}";
  }

  @Test
  void pageFiltersByTopicLevelAndShowsCounters() {
    question(TOPIC_CS, LEVEL_EASY, "alpha", "x");
    question(TOPIC_DESIGN, LEVEL_MEDIUM, "beta", "x");
    retired(TOPIC_DESIGN, LEVEL_MEDIUM, "retired one");
    AdminQuestionPage page =
        repository.page(
            new QuestionListCriteria(
                null, TOPIC_DESIGN, null, QuestionSort.DISPLAY_NO, false, 1, 8));
    assertThat(page.items()).extracting(AdminQuestionRow::title).containsExactly("beta");
    assertThat(page.shown()).isEqualTo(1);
    assertThat(page.total()).isEqualTo(2);
    var byLevel =
        repository.page(
            new QuestionListCriteria(null, null, LEVEL_EASY, QuestionSort.DISPLAY_NO, false, 1, 8));
    assertThat(byLevel.items()).extracting(AdminQuestionRow::title).containsExactly("alpha");
  }

  @Test
  void pageSearchIsAccentInsensitivePrefixMatch() {
    question(TOPIC_CS, LEVEL_EASY, "Kiến trúc phần mềm", "x");
    question(TOPIC_CS, LEVEL_EASY, "Unrelated", "kien truc in content only");
    question(TOPIC_CS, LEVEL_EASY, "Other", "nothing");
    var page =
        repository.page(
            new QuestionListCriteria(
                "kien:* & truc:*", null, null, QuestionSort.DISPLAY_NO, false, 1, 8));
    assertThat(page.items())
        .extracting(AdminQuestionRow::title)
        .containsExactly("Kiến trúc phần mềm", "Unrelated");
  }

  @Test
  void pageClampsPageNumberAndPaginates() {
    for (int i = 0; i < 3; i++) {
      question(TOPIC_CS, LEVEL_EASY, "q" + i, "x");
    }
    var last =
        repository.page(
            new QuestionListCriteria(null, null, null, QuestionSort.DISPLAY_NO, false, 99, 2));
    assertThat(last.number()).isEqualTo(2);
    assertThat(last.items()).extracting(AdminQuestionRow::title).containsExactly("q2");
    var empty =
        repository.page(
            new QuestionListCriteria("zzzz:*", null, null, QuestionSort.DISPLAY_NO, false, 5, 8));
    assertThat(empty.number()).isEqualTo(1);
    assertThat(empty.items()).isEmpty();
  }

  @ParameterizedTest
  @EnumSource(QuestionSort.class)
  void everyWhitelistedSortColumnIsValidSql(QuestionSort sort) {
    question(TOPIC_CS, LEVEL_EASY, "a", "x");
    question(TOPIC_DESIGN, LEVEL_MEDIUM, "b", "x");
    assertThat(
            repository.page(new QuestionListCriteria(null, null, null, sort, false, 1, 8)).items())
        .hasSize(2);
    assertThat(
            repository.page(new QuestionListCriteria(null, null, null, sort, true, 1, 8)).items())
        .hasSize(2);
  }

  @Test
  void sortDescendingByTitleAndLevel() {
    question(TOPIC_CS, LEVEL_EASY, "Apple", "x");
    question(TOPIC_CS, LEVEL_MEDIUM, "banana", "x");
    var byTitle =
        repository.page(new QuestionListCriteria(null, null, null, QuestionSort.TITLE, true, 1, 8));
    assertThat(byTitle.items())
        .extracting(AdminQuestionRow::title)
        .containsExactly("banana", "Apple");
    var byLevel =
        repository.page(new QuestionListCriteria(null, null, null, QuestionSort.LEVEL, true, 1, 8));
    assertThat(byLevel.items().get(0).levelCode()).isEqualTo("MEDIUM");
  }

  @Test
  void usageAndAverageScoreFollowTheBrIb10Formula() {
    UUID q = question(TOPIC_CS, LEVEL_EASY, "scored", "x");
    UUID user = UUID.randomUUID();
    // 80% * 60 + 50% * 40 = 68.0 ; 100% * 60 + 100% * 40 = 100.0 ; average 84.0
    answer(user, q, 1, "COMPLETED", feedback(80, 60, 50, 40));
    answer(user, q, 2, "COMPLETED", feedback(100, 60, 100, 40));
    // A FAILED attempt counts as usage but not as a score.
    answer(user, q, 3, "FAILED", null);
    var row =
        repository
            .page(new QuestionListCriteria(null, null, null, QuestionSort.USAGE_COUNT, true, 1, 8))
            .items()
            .get(0);
    assertThat(row.usageCount()).isEqualTo(3);
    assertThat(row.averageScorePercent()).isEqualByComparingTo("84.0");
    AdminQuestion detail = repository.findActive(q).orElseThrow();
    assertThat(detail.usageCount()).isEqualTo(3);
    assertThat(detail.averageScorePercent()).isEqualByComparingTo("84.0");
  }

  @Test
  void findActiveHidesRetiredAndReturnsRubricInOrder() {
    UUID q = withJson(question(TOPIC_CS, LEVEL_EASY, "with rubric", "x"), "[\"f1\"]", null);
    rubric(q, 2, "second", 40);
    rubric(q, 1, "first", 60);
    AdminQuestion found = repository.findActive(q).orElseThrow();
    assertThat(found.rubric())
        .extracting(RubricCriterion::criterionCode)
        .containsExactly("first", "second");
    assertThat(found.followUps()).containsExactly("f1");
    assertThat(found.usageCount()).isZero();
    assertThat(found.averageScorePercent()).isNull();
    repository.retire(q);
    assertThat(repository.findActive(q)).isEmpty();
    assertThat(repository.findActive(UUID.randomUUID())).isEmpty();
  }

  @ParameterizedTest
  @EnumSource(CatalogKind.class)
  void lockReferenceReportsExistence(CatalogKind kind) {
    UUID existing = kind == CatalogKind.TOPIC ? TOPIC_CS : LEVEL_EASY;
    assertThat(repository.lockReference(kind, existing)).isTrue();
    assertThat(repository.lockReference(kind, UUID.randomUUID())).isFalse();
  }

  @Test
  void lockActiveSkipsRetiredAndLockAllReturnsAnyStatus() {
    UUID active = question(TOPIC_CS, LEVEL_EASY, "active", "x");
    UUID retired = retired(TOPIC_CS, LEVEL_EASY, "retired");
    assertThat(repository.lockActive(active)).isPresent();
    assertThat(repository.lockActive(retired)).isEmpty();
    var all = repository.lockAll(List.of(active, retired, UUID.randomUUID()));
    assertThat(all).hasSize(2);
    assertThat(all).extracting(r -> r.status()).containsExactlyInAnyOrder("ACTIVE", "RETIRED");
  }

  @Test
  void insertStoresQuestionRubricAndCsvOnlyColumns() {
    var rubric =
        List.of(
            new RubricCriterion("clarity", new BigDecimal("60.5"), "desc"),
            new RubricCriterion("depth", new BigDecimal("39.5"), null));
    AdminQuestion created = repository.insert(draft("New question", rubric), actor);
    assertThat(created.displayNo()).isEqualTo(1);
    assertThat(created.rubric())
        .extracting(RubricCriterion::criterionCode)
        .containsExactly("clarity", "depth");
    assertThat(created.followUps()).containsExactly("why?", "how?");
    assertThat(
            jdbc.sql(
                    "SELECT core_keywords::text FROM interview_bank.interview_questions"
                        + " WHERE id = :id")
                .param("id", created.id())
                .query(String.class)
                .single())
        .contains("kw");
    var noKeywords =
        new QuestionDraft(
            TOPIC_CS, LEVEL_EASY, "No kw", null, List.of(), List.of(), null, null, null);
    assertThat(repository.insert(noKeywords, actor).displayNo()).isEqualTo(2);
  }

  @Test
  void updateChangesFormFieldsOnlyAndReplaceRubricRewritesRows() {
    UUID q = repository.insert(draft("Before", List.of()), actor).id();
    var changed =
        new QuestionDraft(
            TOPIC_DESIGN,
            LEVEL_MEDIUM,
            "After",
            "new body",
            List.of("only"),
            List.of(),
            "IGNORED",
            null,
            null);
    repository.update(q, changed);
    AdminQuestion after = repository.findActive(q).orElseThrow();
    assertThat(after.title()).isEqualTo("After");
    assertThat(after.topicId()).isEqualTo(TOPIC_DESIGN);
    assertThat(after.followUps()).containsExactly("only");
    assertThat(
            jdbc.sql(
                    "SELECT suggested_approach FROM interview_bank.interview_questions"
                        + " WHERE id = :id")
                .param("id", q)
                .query(String.class)
                .single())
        .isEqualTo("approach");
    repository.replaceRubric(q, List.of(new RubricCriterion("a", new BigDecimal("100"), null)));
    repository.replaceRubric(
        q,
        List.of(
            new RubricCriterion("b", new BigDecimal("50"), null),
            new RubricCriterion("c", new BigDecimal("50"), null)));
    assertThat(repository.findActive(q).orElseThrow().rubric())
        .extracting(RubricCriterion::criterionCode)
        .containsExactly("b", "c");
  }

  @Test
  void recalculateProgressCountsDistinctActiveQuestionsAndResetsWhenNoneLeft() {
    UUID q1 = question(TOPIC_CS, LEVEL_EASY, "one", "x");
    UUID q2 = question(TOPIC_CS, LEVEL_EASY, "two", "x");
    UUID user = UUID.randomUUID();
    answer(user, q1, 1, "FAILED", null);
    answer(user, q1, 2, "FAILED", null);
    answer(user, q2, 1, "FAILED", null);
    repository.recalculateProgress(q1, List.of(TOPIC_CS, TOPIC_CS));
    assertThat(attempted(user)).isEqualTo(2);
    repository.retire(q1);
    repository.recalculateProgress(q1, List.of(TOPIC_CS));
    assertThat(attempted(user)).isEqualTo(1);
    repository.retire(q2);
    repository.recalculateProgress(q1, List.of(TOPIC_CS));
    assertThat(attempted(user)).isZero();
    assertThat(
            jdbc.sql("SELECT last_practiced_at FROM interview_bank.practice_history")
                .query(java.time.OffsetDateTime.class)
                .optional())
        .isEmpty();
  }

  private int attempted(UUID user) {
    return jdbc.sql(
            "SELECT questions_attempted FROM interview_bank.practice_history WHERE user_id = :u")
        .param("u", user)
        .query(Integer.class)
        .single();
  }

  @Test
  void recalculateWithoutPriorAnswersWritesNothing() {
    UUID q = question(TOPIC_CS, LEVEL_EASY, "lonely", "x");
    repository.recalculateProgress(q, List.of(TOPIC_CS));
    assertThat(count("practice_history")).isZero();
  }

  @Test
  void importHelpersNormaliseTitlesLikeTheDatabase() {
    question(TOPIC_CS, LEVEL_EASY, "  Mixed Case  ", "x");
    retired(TOPIC_CS, LEVEL_EASY, "Gone");
    repository.lockImport();
    assertThat(repository.activeTitleKeys()).containsExactly("mixed case");
    assertThat(repository.titleKey("  ABC ")).isEqualTo("abc");
  }
}
