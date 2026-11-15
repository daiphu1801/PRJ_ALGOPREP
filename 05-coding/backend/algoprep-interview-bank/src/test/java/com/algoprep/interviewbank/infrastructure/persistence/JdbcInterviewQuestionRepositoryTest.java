package com.algoprep.interviewbank.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.interviewbank.domain.model.LearnerQuestion;
import com.algoprep.interviewbank.domain.model.LearnerQuestionCriteria;
import com.algoprep.interviewbank.domain.model.LearnerQuestionPage;
import com.algoprep.interviewbank.domain.model.LearnerQuestionRow;
import com.algoprep.interviewbank.domain.model.QuestionSearch;
import com.algoprep.interviewbank.domain.model.RecallFilter;
import com.algoprep.interviewbank.domain.model.RecallRating;
import com.algoprep.interviewbank.domain.model.RecallState;
import com.algoprep.interviewbank.domain.model.TopicProgress;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Real PostgreSQL; study-mode reads (BR-IB-04 search, recall scheduling, bookmarks, progress). */
class JdbcInterviewQuestionRepositoryTest extends PostgresTestBase {
  private final JdbcInterviewQuestionRepository repository =
      new JdbcInterviewQuestionRepository(provider(), ids);
  private final JdbcInterviewTopicProgressRepository progress =
      new JdbcInterviewTopicProgressRepository(provider());
  private final UUID user = UUID.randomUUID();

  private LearnerQuestionCriteria criteria(
      String text, RecallFilter status, boolean bookmarked, int page, int size) {
    String ts = QuestionSearch.tsQuery(text);
    return new LearnerQuestionCriteria(
        user,
        ts,
        ts == null ? null : QuestionSearch.escapeLike(text),
        null,
        null,
        status,
        bookmarked,
        page,
        size,
        false);
  }

  private LearnerQuestionCriteria drillCriteria(int page, int size) {
    return new LearnerQuestionCriteria(
        user, null, null, null, null, RecallFilter.ALL, false, page, size, true);
  }

  @Test
  void pageExcludesRetiredAndReportsShownVersusTotal() {
    question(TOPIC_CS, LEVEL_EASY, "visible", "x");
    retired(TOPIC_CS, LEVEL_EASY, "retired");
    LearnerQuestionPage page = repository.page(criteria(null, RecallFilter.ALL, false, 1, 20));
    assertThat(page.items()).extracting(LearnerQuestionRow::title).containsExactly("visible");
    assertThat(page.shown()).isEqualTo(1);
    assertThat(page.total()).isEqualTo(1);
    assertThat(page.items().get(0).suggestedApproach()).isNull();
    assertThat(page.items().get(0).coreKeywords()).isEmpty();
  }

  @Test
  void pageFiltersByTopicAndLevelIds() {
    question(TOPIC_CS, LEVEL_EASY, "cs easy", "x");
    question(TOPIC_DESIGN, LEVEL_MEDIUM, "design medium", "x");
    var byTopic =
        new LearnerQuestionCriteria(
            user, null, null, TOPIC_DESIGN, null, RecallFilter.ALL, false, 1, 20, false);
    assertThat(repository.page(byTopic).items())
        .extracting(LearnerQuestionRow::title)
        .containsExactly("design medium");
    var byLevel =
        new LearnerQuestionCriteria(
            user, null, null, null, LEVEL_EASY, RecallFilter.ALL, false, 1, 20, false);
    assertThat(repository.page(byLevel).items())
        .extracting(LearnerQuestionRow::title)
        .containsExactly("cs easy");
  }

  @Test
  void searchMatchesTitleContentAndKeywordsWithLikeEscaping() {
    question(TOPIC_CS, LEVEL_EASY, "Hàm băm", "x");
    question(TOPIC_CS, LEVEL_EASY, "Other", "hash tables explained");
    withJson(question(TOPIC_CS, LEVEL_EASY, "Third", "nothing"), "[]", "[\"100%_done\"]");
    assertThat(repository.page(criteria("ham bam", RecallFilter.ALL, false, 1, 20)).items())
        .extracting(LearnerQuestionRow::title)
        .containsExactly("Hàm băm");
    assertThat(repository.page(criteria("hash", RecallFilter.ALL, false, 1, 20)).items())
        .extracting(LearnerQuestionRow::title)
        .containsExactly("Other");
    // Underscore and percent must be matched literally in the keyword LIKE branch.
    assertThat(repository.page(criteria("100%_done", RecallFilter.ALL, false, 1, 20)).items())
        .extracting(LearnerQuestionRow::title)
        .containsExactly("Third");
    assertThat(repository.page(criteria("1%0", RecallFilter.ALL, false, 1, 20)).items()).isEmpty();
  }

  @ParameterizedTest
  @EnumSource(RecallFilter.class)
  void everyStatusTabFiltersOnTheRecallRating(RecallFilter filter) {
    UUID unseen = question(TOPIC_CS, LEVEL_EASY, "unseen", "x");
    UUID known = question(TOPIC_CS, LEVEL_EASY, "known", "x");
    UUID vague = question(TOPIC_CS, LEVEL_EASY, "vague", "x");
    UUID forgotten = question(TOPIC_CS, LEVEL_EASY, "forgotten", "x");
    repository.rate(user, known, RecallRating.KNOWN);
    repository.rate(user, vague, RecallRating.VAGUE);
    repository.rate(user, forgotten, RecallRating.FORGOTTEN);
    // Another learner's rating must not leak into this learner's tabs.
    repository.rate(UUID.randomUUID(), unseen, RecallRating.KNOWN);
    List<String> expected =
        switch (filter) {
          case ALL -> List.of("unseen", "known", "vague", "forgotten");
          case UNSEEN -> List.of("unseen");
          case REVIEWING -> List.of("vague", "forgotten");
          case KNOWN -> List.of("known");
        };
    assertThat(repository.page(criteria(null, filter, false, 1, 20)).items())
        .extracting(LearnerQuestionRow::title)
        .containsExactlyElementsOf(expected);
  }

  @Test
  void bookmarkedOnlyAndPageClamping() {
    UUID a = question(TOPIC_CS, LEVEL_EASY, "a", "x");
    question(TOPIC_CS, LEVEL_EASY, "b", "x");
    repository.setBookmark(user, a, true);
    var bookmarked = repository.page(criteria(null, RecallFilter.ALL, true, 1, 20));
    assertThat(bookmarked.items()).extracting(LearnerQuestionRow::title).containsExactly("a");
    assertThat(bookmarked.items().get(0).bookmarked()).isTrue();
    var clamped = repository.page(criteria(null, RecallFilter.ALL, false, 50, 1));
    assertThat(clamped.number()).isEqualTo(2);
    assertThat(clamped.items()).extracting(LearnerQuestionRow::title).containsExactly("b");
  }

  @Test
  void drillOrdersDueThenNeverRatedThenFutureAndCarriesApproachAndKeywords() {
    UUID future = withJson(question(TOPIC_CS, LEVEL_EASY, "future", "x"), "[]", "[\"k1\",\"k2\"]");
    question(TOPIC_CS, LEVEL_EASY, "fresh", "x");
    UUID due = question(TOPIC_CS, LEVEL_EASY, "due", "x");
    repository.rate(user, future, RecallRating.KNOWN);
    repository.rate(user, due, RecallRating.FORGOTTEN);
    jdbc.sql(
            "UPDATE interview_bank.recall_ratings SET next_review_at = now() - interval '1 day'"
                + " WHERE question_id = :q")
        .param("q", due)
        .update();
    var page = repository.page(drillCriteria(7, 10));
    assertThat(page.items())
        .extracting(LearnerQuestionRow::title)
        .containsExactly("due", "fresh", "future");
    assertThat(page.number()).isEqualTo(1);
    assertThat(page.shown()).isEqualTo(3);
    var futureRow = page.items().get(2);
    assertThat(futureRow.suggestedApproach()).isEqualTo("approach");
    assertThat(futureRow.coreKeywords()).containsExactly("k1", "k2");
  }

  @Test
  void findReturnsFullDetailWithRecallBookmarkAndPracticeFlag() {
    UUID q =
        withJson(
            question(TOPIC_BEHAVIORAL, LEVEL_MEDIUM, "detail", "content"), "[\"f\"]", "[\"kw\"]");
    LearnerQuestion plain = repository.find(user, q).orElseThrow();
    assertThat(plain.usesStarFramework()).isTrue();
    assertThat(plain.followUps()).containsExactly("f");
    assertThat(plain.coreKeywords()).containsExactly("kw");
    assertThat(plain.recall()).isNull();
    assertThat(plain.bookmarked()).isFalse();
    assertThat(plain.practiceAvailable()).isFalse();
    rubric(q, 1, "c", 100);
    repository.setBookmark(user, q, true);
    repository.rate(user, q, RecallRating.VAGUE);
    LearnerQuestion full = repository.find(user, q).orElseThrow();
    assertThat(full.practiceAvailable()).isTrue();
    assertThat(full.bookmarked()).isTrue();
    assertThat(full.recall().rating()).isEqualTo(RecallRating.VAGUE);
    // Null JSON columns read as empty lists.
    UUID bare = question(TOPIC_CS, LEVEL_EASY, "bare", null);
    assertThat(repository.find(user, bare).orElseThrow().coreKeywords()).isEmpty();
  }

  @Test
  void findHidesRetiredAndUnknown() {
    UUID retired = retired(TOPIC_CS, LEVEL_EASY, "r");
    assertThat(repository.find(user, retired)).isEmpty();
    assertThat(repository.find(user, UUID.randomUUID())).isEmpty();
    assertThat(repository.isActive(retired)).isFalse();
  }

  @Test
  void rateSchedulesNextReviewByIntervalAndOverwritesInPlace() {
    UUID q = question(TOPIC_CS, LEVEL_EASY, "rated", "x");
    assertThat(repository.isActive(q)).isTrue();
    RecallState first = repository.rate(user, q, RecallRating.KNOWN).orElseThrow();
    assertThat(first.intervalDays()).isEqualTo(7);
    assertThat(ChronoUnit.DAYS.between(first.ratedAt(), first.nextReviewAt())).isEqualTo(7);
    RecallState second = repository.rate(user, q, RecallRating.FORGOTTEN).orElseThrow();
    assertThat(second.rating()).isEqualTo(RecallRating.FORGOTTEN);
    assertThat(second.intervalDays()).isEqualTo(1);
    assertThat(second.nextReviewAt()).isAfter(Instant.now().minus(1, ChronoUnit.MINUTES));
    assertThat(count("recall_ratings")).isEqualTo(1);
  }

  @Test
  void rateRefusesRetiredAndUnknownQuestionsInOneStatement() {
    UUID retired = retired(TOPIC_CS, LEVEL_EASY, "r");
    assertThat(repository.rate(user, retired, RecallRating.KNOWN)).isEmpty();
    assertThat(repository.rate(user, UUID.randomUUID(), RecallRating.KNOWN)).isEmpty();
    assertThat(count("recall_ratings")).isZero();
  }

  @Test
  void bookmarkIsIdempotentBothWays() {
    UUID q = question(TOPIC_CS, LEVEL_EASY, "b", "x");
    repository.setBookmark(user, q, true);
    repository.setBookmark(user, q, true);
    assertThat(count("bookmarks")).isEqualTo(1);
    repository.setBookmark(user, q, false);
    repository.setBookmark(user, q, false);
    assertThat(count("bookmarks")).isZero();
  }

  @Test
  void topicProgressListsEveryTopicWithOwnHistoryAndActiveTotals() {
    question(TOPIC_CS, LEVEL_EASY, "a", "x");
    question(TOPIC_CS, LEVEL_EASY, "b", "x");
    retired(TOPIC_CS, LEVEL_EASY, "gone");
    history(user, 1);
    history(UUID.randomUUID(), 2);
    List<TopicProgress> rows = progress.listFor(user);
    assertThat(rows).hasSize(5);
    assertThat(rows).extracting(TopicProgress::sortOrder).isSorted();
    TopicProgress cs = rows.get(0);
    assertThat(cs.topicCode()).isEqualTo("CS_THEORY");
    assertThat(cs.questionsAttempted()).isEqualTo(1);
    assertThat(cs.totalQuestions()).isEqualTo(2);
    assertThat(cs.completionPercent()).isEqualTo(50);
    assertThat(cs.lastPracticedAt()).isNotNull();
    TopicProgress untouched = rows.get(1);
    assertThat(untouched.questionsAttempted()).isZero();
    assertThat(untouched.completionPercent()).isZero();
    assertThat(untouched.lastPracticedAt()).isNull();
  }

  private void history(UUID owner, int attempted) {
    jdbc.sql(
            "INSERT INTO interview_bank.practice_history(id, user_id, topic_id,"
                + " questions_attempted, last_practiced_at) VALUES (:id, :u, :t, :n, now())")
        .param("id", UUID.randomUUID())
        .param("u", owner)
        .param("t", TOPIC_CS)
        .param("n", attempted)
        .update();
  }
}
