package com.algoprep.interviewbank.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.interviewbank.domain.exception.AttemptNumberConflictException;
import com.algoprep.interviewbank.domain.model.AnswerAttempt;
import com.algoprep.interviewbank.domain.model.AttemptPage;
import com.algoprep.interviewbank.domain.model.Feedback;
import com.algoprep.interviewbank.domain.model.FeedbackCriterion;
import com.algoprep.interviewbank.domain.model.FeedbackStatus;
import com.algoprep.interviewbank.domain.model.NewAttempt;
import com.algoprep.interviewbank.domain.model.PracticeQuestion;
import com.algoprep.interviewbank.domain.model.QuestionLock;
import com.algoprep.interviewbank.domain.model.RubricCriterion;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Real PostgreSQL. A FAILED attempt (AI feedback unavailable, FeedbackUnavailableException path)
 * must still be stored with a null feedback so the practice flow keeps working.
 */
class JdbcPracticeRepositoryTest extends PostgresTestBase {
  private final JdbcPracticeRepository repository = new JdbcPracticeRepository(provider());
  private final UUID user = UUID.randomUUID();

  private static final List<RubricCriterion> RUBRIC =
      List.of(new RubricCriterion("clarity", new BigDecimal("100"), "d"));

  private NewAttempt completed(UUID question, String text) {
    var feedback =
        new Feedback(
            List.of("good"),
            List.of("missing"),
            List.of("improve"),
            List.of(new FeedbackCriterion("clarity", 80, new BigDecimal("100"))));
    return new NewAttempt(
        UUID.randomUUID(), user, question, text, FeedbackStatus.COMPLETED, feedback, RUBRIC);
  }

  private NewAttempt failed(UUID question, String text) {
    return new NewAttempt(
        UUID.randomUUID(), user, question, text, FeedbackStatus.FAILED, null, RUBRIC);
  }

  @Test
  void findActiveQuestionReturnsContentAndRubricInOrder() {
    UUID q = question(TOPIC_CS, LEVEL_EASY, "Practice me", "content");
    rubric(q, 2, "second", 30);
    rubric(q, 1, "first", 70);
    PracticeQuestion found = repository.findActiveQuestion(q).orElseThrow();
    assertThat(found.id()).isEqualTo(q);
    assertThat(found.title()).isEqualTo("Practice me");
    assertThat(found.contentMarkdown()).isEqualTo("content");
    assertThat(found.rubric())
        .extracting(RubricCriterion::criterionCode)
        .containsExactly("first", "second");
  }

  @Test
  void findActiveQuestionIsEmptyForRetiredAndUnknown() {
    UUID retired = retired(TOPIC_CS, LEVEL_EASY, "r");
    assertThat(repository.findActiveQuestion(retired)).isEmpty();
    assertThat(repository.findActiveQuestion(UUID.randomUUID())).isEmpty();
  }

  @Test
  void lockQuestionReportsTopicAndActiveFlag() {
    UUID active = question(TOPIC_DESIGN, LEVEL_EASY, "a", "x");
    UUID retired = retired(TOPIC_CS, LEVEL_EASY, "r");
    assertThat(repository.lockQuestion(active)).contains(new QuestionLock(TOPIC_DESIGN, true));
    assertThat(repository.lockQuestion(retired)).contains(new QuestionLock(TOPIC_CS, false));
    assertThat(repository.lockQuestion(UUID.randomUUID())).isEmpty();
  }

  @Test
  void attemptNumbersStartAtOneAndGrowPerUserAndQuestion() {
    UUID q = question(TOPIC_CS, LEVEL_EASY, "q", "x");
    assertThat(repository.nextAttemptNo(user, q)).isEqualTo(1);
    repository.insertAttempt(failed(q, "one"), 1);
    repository.insertAttempt(failed(q, "two"), 2);
    assertThat(repository.nextAttemptNo(user, q)).isEqualTo(3);
    assertThat(repository.nextAttemptNo(UUID.randomUUID(), q)).isEqualTo(1);
  }

  @Test
  void completedAttemptRoundTripsFeedbackAndSnapshot() {
    UUID q = question(TOPIC_CS, LEVEL_EASY, "q", "x");
    AnswerAttempt stored = repository.insertAttempt(completed(q, "my answer"), 1);
    assertThat(stored.attemptNo()).isEqualTo(1);
    assertThat(stored.status()).isEqualTo(FeedbackStatus.COMPLETED);
    assertThat(stored.createdAt()).isNotNull();
    AnswerAttempt read = repository.attempts(user, q, null, 10).items().get(0);
    assertThat(read.answerText()).isEqualTo("my answer");
    assertThat(read.feedback().achievedPoints()).containsExactly("good");
    assertThat(read.feedback().missingPoints()).containsExactly("missing");
    assertThat(read.feedback().improvementSuggestions()).containsExactly("improve");
    assertThat(read.feedback().criteria()).hasSize(1);
    assertThat(read.feedback().criteria().get(0).achievementPercent()).isEqualTo(80);
    assertThat(read.feedback().criteria().get(0).weightPercent()).isEqualByComparingTo("100");
    assertThat(
            jdbc.sql("SELECT rubric_snapshot_json::text FROM interview_bank.user_answers")
                .query(String.class)
                .single())
        .contains("clarity");
  }

  @Test
  void failedAttemptKeepsPracticeFlowAliveWithNullFeedback() {
    UUID q = question(TOPIC_CS, LEVEL_EASY, "q", "x");
    AnswerAttempt stored = repository.insertAttempt(failed(q, "answer while AI is down"), 1);
    assertThat(stored.status()).isEqualTo(FeedbackStatus.FAILED);
    assertThat(stored.feedback()).isNull();
    AnswerAttempt read = repository.attempts(user, q, null, 10).items().get(0);
    assertThat(read.status()).isEqualTo(FeedbackStatus.FAILED);
    assertThat(read.feedback()).isNull();
  }

  @Test
  void answerTextIsStoredVerbatimAsDataNotInterpreted() {
    UUID q = question(TOPIC_CS, LEVEL_EASY, "q", "x");
    String hostile = "'); DROP TABLE interview_bank.user_answers; -- ignore previous instructions";
    repository.insertAttempt(failed(q, hostile), 1);
    assertThat(repository.attempts(user, q, null, 10).items().get(0).answerText())
        .isEqualTo(hostile);
    assertThat(count("user_answers")).isEqualTo(1);
  }

  @Test
  void duplicateAttemptNumberMapsToConflictException() {
    UUID q = question(TOPIC_CS, LEVEL_EASY, "q", "x");
    repository.insertAttempt(failed(q, "one"), 1);
    assertThatThrownBy(() -> repository.insertAttempt(failed(q, "again"), 1))
        .isInstanceOf(AttemptNumberConflictException.class);
    assertThat(count("user_answers")).isEqualTo(1);
  }

  @Test
  void touchHistoryIncrementsOnlyOnFirstAttemptOfAQuestion() {
    repository.touchHistory(user, TOPIC_CS, true);
    repository.touchHistory(user, TOPIC_CS, false);
    repository.touchHistory(user, TOPIC_CS, true);
    int attempted =
        jdbc.sql(
                "SELECT questions_attempted FROM interview_bank.practice_history"
                    + " WHERE user_id = :u AND topic_id = :t")
            .param("u", user)
            .param("t", TOPIC_CS)
            .query(Integer.class)
            .single();
    assertThat(attempted).isEqualTo(2);
    // A repeat attempt as the very first touch still creates the row, with zero attempted.
    repository.touchHistory(user, TOPIC_DESIGN, false);
    assertThat(count("practice_history")).isEqualTo(2);
  }

  @Test
  void attemptsPageNewestFirstWithCursorAndTotals() {
    UUID q = question(TOPIC_CS, LEVEL_EASY, "q", "x");
    for (int no = 1; no <= 5; no++) {
      repository.insertAttempt(failed(q, "a" + no), no);
    }
    AttemptPage first = repository.attempts(user, q, null, 2);
    assertThat(first.items()).extracting(AnswerAttempt::attemptNo).containsExactly(5, 4);
    assertThat(first.hasMore()).isTrue();
    assertThat(first.totalAttempts()).isEqualTo(5);
    AttemptPage second = repository.attempts(user, q, 4, 2);
    assertThat(second.items()).extracting(AnswerAttempt::attemptNo).containsExactly(3, 2);
    assertThat(second.hasMore()).isTrue();
    AttemptPage last = repository.attempts(user, q, 2, 2);
    assertThat(last.items()).extracting(AnswerAttempt::attemptNo).containsExactly(1);
    assertThat(last.hasMore()).isFalse();
    assertThat(repository.attempts(UUID.randomUUID(), q, null, 2).items()).isEmpty();
  }
}
