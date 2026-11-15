package com.algoprep.aireview.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.aireview.domain.model.IdleSession;
import com.algoprep.aireview.domain.model.InterviewEntryType;
import com.algoprep.aireview.domain.model.InterviewSession;
import com.algoprep.aireview.domain.model.InterviewSessionStage;
import com.algoprep.aireview.domain.model.InterviewStage;
import com.algoprep.aireview.domain.model.InterviewTurn;
import com.algoprep.aireview.domain.model.InterviewerLevel;
import com.algoprep.aireview.domain.model.RubricCriterion;
import com.algoprep.aireview.domain.model.RubricRow;
import com.algoprep.aireview.domain.model.RubricScore;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

/** Interview session, turn and scoring repositories against real PostgreSQL. */
class InterviewRepositoriesPgTest extends PostgresTestBase {
  private final JdbcInterviewSessionRepository sessions =
      new JdbcInterviewSessionRepository(provider());
  private final JdbcInterviewTurnRepository turns = new JdbcInterviewTurnRepository(provider());
  private final JdbcInterviewScoringRepository scoring =
      new JdbcInterviewScoringRepository(provider());

  private static InterviewTurn turn(
      UUID session, int index, InterviewTurn.Role role, InterviewStage stage, Instant at) {
    return new InterviewTurn(UUID.randomUUID(), session, index, role, stage, "text " + index, at);
  }

  private static InterviewSession topicSession(UUID user, int maxTurns, boolean hints, Instant at) {
    return new InterviewSession(
        UUID.randomUUID(),
        user,
        InterviewEntryType.TOPIC_SELECTION,
        null,
        null,
        null,
        List.of("graphs", "dp"),
        InterviewerLevel.MIDDLE,
        maxTurns,
        hints,
        0,
        InterviewSessionStage.EXPLAIN,
        0,
        at,
        null,
        at);
  }

  private InterviewSession open(InterviewSession session) {
    sessions.insertOpened(
        session,
        turn(session.id(), 1, InterviewTurn.Role.AI, InterviewStage.EXPLAIN, session.startedAt()));
    return session;
  }

  /** Raw SUBMISSION-entry session; terminal stages get ended_at (table CHECK). */
  private UUID raw(UUID user, String stage, Instant started, Instant updated) {
    UUID id = UUID.randomUUID();
    boolean terminal = stage.equals("COMPLETED") || stage.equals("ABANDONED");
    jdbc.sql(
            "INSERT INTO ai.interview_sessions(id,user_id,entry_type,submission_id,stage,"
                + "started_at,updated_at,ended_at) VALUES(:id,:u,'SUBMISSION',:s,:stage,:st,:up,:end)")
        .param("id", id)
        .param("u", user)
        .param("s", UUID.randomUUID())
        .param("stage", stage)
        .param("st", utc(started))
        .param("up", utc(updated))
        .param("end", terminal ? utc(updated) : null, java.sql.Types.TIMESTAMP_WITH_TIMEZONE)
        .update();
    return id;
  }

  private void rubric(UUID session, RubricCriterion criterion, String score) {
    scoring.insertRubric(
        session,
        List.of(new RubricScore(criterion, new BigDecimal(score), "c")),
        now(),
        UUID::randomUUID);
  }

  private String column(UUID id, String column) {
    return jdbc.sql(
            "SELECT COALESCE("
                + column
                + "::text, 'NULL') FROM ai.interview_sessions WHERE id = :id")
        .param("id", id)
        .query(String.class)
        .single();
  }

  @Test
  void topicsAreStoredAsJsonbArrayOnlyForTopicSelection() {
    Instant at = now();
    UUID user = UUID.randomUUID();
    InterviewSession topic = open(topicSession(user, 5, true, at));
    InterviewSession bank =
        open(
            new InterviewSession(
                UUID.randomUUID(),
                user,
                InterviewEntryType.QUESTION_BANK,
                null,
                "q-1",
                "Explain hashing",
                List.of("ignored"),
                InterviewerLevel.JUNIOR,
                4,
                false,
                0,
                InterviewSessionStage.EXPLAIN,
                0,
                at.plusSeconds(1),
                null,
                at.plusSeconds(1)));
    InterviewSession submission =
        open(
            new InterviewSession(
                UUID.randomUUID(),
                user,
                InterviewEntryType.SUBMISSION,
                UUID.randomUUID(),
                null,
                null,
                List.of(),
                null,
                null,
                null,
                0,
                InterviewSessionStage.EXPLAIN,
                0,
                at.plusSeconds(2),
                null,
                at.plusSeconds(2)));

    assertThat(column(topic.id(), "jsonb_typeof(topics)")).isEqualTo("array");
    assertThat(column(topic.id(), "topics")).isEqualTo("[\"graphs\", \"dp\"]");
    assertThat(column(bank.id(), "topics")).isEqualTo("NULL");
    assertThat(column(submission.id(), "topics")).isEqualTo("NULL");
    assertThat(count("interview_turns")).isEqualTo(3);
  }

  @Test
  void findMapsEveryEntryTypeBackWithNullableSettings() {
    Instant at = now();
    UUID user = UUID.randomUUID();
    InterviewSession topic = open(topicSession(user, 5, true, at));
    UUID submissionId = UUID.randomUUID();
    InterviewSession submission =
        open(
            new InterviewSession(
                UUID.randomUUID(),
                user,
                InterviewEntryType.SUBMISSION,
                submissionId,
                null,
                null,
                List.of(),
                null,
                null,
                null,
                0,
                InterviewSessionStage.EXPLAIN,
                0,
                at.plusSeconds(1),
                null,
                at.plusSeconds(1)));

    assertThat(sessions.find(topic.id())).contains(topic);
    assertThat(sessions.find(submission.id())).contains(submission);
    assertThat(sessions.find(UUID.randomUUID())).isEmpty();
    assertThat(sessions.findAllByUser(user)).containsExactly(topic, submission);
    assertThat(sessions.findAllByUser(UUID.randomUUID())).isEmpty();
  }

  @Test
  void insertOpenedIsAtomicWhenTheOpeningTurnViolatesTheForeignKey() {
    InterviewSession session = topicSession(UUID.randomUUID(), 5, true, now());
    InterviewTurn orphan =
        turn(UUID.randomUUID(), 1, InterviewTurn.Role.AI, InterviewStage.EXPLAIN, now());

    assertThatThrownBy(
            () -> tx.executeWithoutResult(status -> sessions.insertOpened(session, orphan)))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThat(count("interview_sessions")).isZero();
    assertThat(count("interview_turns")).isZero();
  }

  @Test
  void entryFieldsCheckRejectsASessionWhoseSettingsDoNotMatchItsEntryType() {
    Instant at = now();
    var badSubmission =
        new InterviewSession(
            UUID.randomUUID(),
            UUID.randomUUID(),
            InterviewEntryType.SUBMISSION,
            null,
            null,
            null,
            List.of(),
            null,
            null,
            null,
            0,
            InterviewSessionStage.EXPLAIN,
            0,
            at,
            null,
            at);

    assertThatThrownBy(() -> open(badSubmission))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(count("interview_sessions")).isZero();
  }

  @Test
  void turnsAreReadInIndexOrderAndScopedBySessionAndOwner() {
    Instant at = now();
    UUID user = UUID.randomUUID();
    InterviewSession first = open(topicSession(user, 9, true, at));
    InterviewSession second = open(topicSession(UUID.randomUUID(), 9, true, at));
    turns.insertTurn(turn(first.id(), 3, InterviewTurn.Role.USER, InterviewStage.EXPLAIN, at));
    turns.insertTurn(turn(first.id(), 2, InterviewTurn.Role.USER, InterviewStage.CHALLENGE, at));

    assertThat(sessions.turns(first.id()))
        .extracting(InterviewTurn::turnIndex)
        .containsExactly(1, 2, 3);
    assertThat(sessions.turns(second.id())).extracting(InterviewTurn::turnIndex).containsExactly(1);
    assertThat(sessions.turnsOfUser(user))
        .extracting(InterviewTurn::turnIndex)
        .containsExactly(1, 2, 3);
    assertThat(sessions.turnsOfUser(user))
        .extracting(InterviewTurn::sessionId)
        .containsOnly(first.id());
  }

  @Test
  void duplicateTurnIndexMapsToDuplicateKeyOnTheRealUniqueIndex() {
    InterviewSession session = open(topicSession(UUID.randomUUID(), 9, true, now()));

    assertThatThrownBy(
            () ->
                turns.insertTurn(
                    turn(session.id(), 1, InterviewTurn.Role.USER, InterviewStage.EXPLAIN, now())))
        .isInstanceOf(DuplicateKeyException.class);
    assertThat(sessions.turns(session.id())).hasSize(1);
  }

  @Test
  void countUserTurnsCountsOnlyUserRowsOfTheAskedStage() {
    Instant at = now();
    InterviewSession session = open(topicSession(UUID.randomUUID(), 9, true, at));
    turns.insertTurn(turn(session.id(), 2, InterviewTurn.Role.USER, InterviewStage.EXPLAIN, at));
    turns.insertTurn(turn(session.id(), 3, InterviewTurn.Role.AI, InterviewStage.EXPLAIN, at));
    turns.insertTurn(turn(session.id(), 4, InterviewTurn.Role.USER, InterviewStage.EXPLAIN, at));
    turns.insertTurn(turn(session.id(), 5, InterviewTurn.Role.USER, InterviewStage.CHALLENGE, at));

    assertThat(sessions.countUserTurns(session.id(), InterviewStage.EXPLAIN)).isEqualTo(2);
    assertThat(sessions.countUserTurns(session.id(), InterviewStage.CHALLENGE)).isEqualTo(1);
    assertThat(sessions.countUserTurns(session.id(), InterviewStage.SCALE_UP)).isZero();
  }

  @Test
  void updateUserContentRewritesOnlyTheUserRowAtThatIndex() {
    Instant at = now();
    InterviewSession session = open(topicSession(UUID.randomUUID(), 9, true, at));
    turns.insertTurn(turn(session.id(), 2, InterviewTurn.Role.USER, InterviewStage.EXPLAIN, at));

    turns.updateUserContent(session.id(), 1, "tampered");
    turns.updateUserContent(session.id(), 2, "edited");

    assertThat(sessions.turns(session.id()))
        .extracting(InterviewTurn::content)
        .containsExactly("text 1", "edited");
  }

  @Test
  void touchAndAdvanceStampUpdatedAtAndAdvanceStopsOnTerminalOrFullSession() {
    Instant at = now().minus(1, ChronoUnit.HOURS);
    InterviewSession session = open(topicSession(UUID.randomUUID(), 1, true, at));
    Instant later = at.plusSeconds(600);

    turns.touch(session.id(), later);
    assertThat(sessions.find(session.id()).orElseThrow().updatedAt()).isEqualTo(later);

    assertThat(turns.advance(session.id(), InterviewSessionStage.CHALLENGE, later.plusSeconds(1)))
        .isTrue();
    InterviewSession advanced = sessions.find(session.id()).orElseThrow();
    assertThat(advanced.turnCount()).isEqualTo(1);
    assertThat(advanced.stage()).isEqualTo(InterviewSessionStage.CHALLENGE);
    assertThat(advanced.updatedAt()).isEqualTo(later.plusSeconds(1));

    // max_turns = 1: the turn_count CHECK is the last backstop behind the service guard.
    assertThatThrownBy(() -> turns.advance(session.id(), InterviewSessionStage.SCALE_UP, later))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(sessions.find(session.id()).orElseThrow().turnCount()).isEqualTo(1);

    UUID done = raw(UUID.randomUUID(), "COMPLETED", at, at);
    assertThat(turns.advance(done, InterviewSessionStage.EXPLAIN, later)).isFalse();
    assertThat(column(done, "turn_count")).isEqualTo("0");
  }

  @Test
  void incrementHintCountStopsAtTheCapAndForTerminalSessions() {
    InterviewSession session = open(topicSession(UUID.randomUUID(), 9, true, now()));

    assertThat(sessions.incrementHintCount(session.id(), 2)).isTrue();
    assertThat(sessions.incrementHintCount(session.id(), 2)).isTrue();
    assertThat(sessions.incrementHintCount(session.id(), 2)).isFalse();
    assertThat(sessions.find(session.id()).orElseThrow().hintCount()).isEqualTo(2);

    UUID done = raw(UUID.randomUUID(), "ABANDONED", now(), now());
    assertThat(sessions.incrementHintCount(done, 2)).isFalse();
    assertThat(sessions.incrementHintCount(UUID.randomUUID(), 2)).isFalse();
  }

  @Test
  void idleGuardClosesOnlyAnUntouchedStaleRunningSession() {
    Instant stale = now().minus(2, ChronoUnit.HOURS);
    Instant closeAt = now();
    UUID user = UUID.randomUUID();
    UUID mismatched = raw(user, "EXPLAIN", stale.minusSeconds(10), stale);
    UUID fresh = raw(user, "CHALLENGE", closeAt, closeAt.minusSeconds(300));
    UUID untouched = raw(user, "SCALE_UP", stale.minusSeconds(10), stale);

    assertThat(scoring.markCompleted(mismatched, closeAt, stale.plusSeconds(1), 30)).isFalse();
    assertThat(column(mismatched, "stage")).isEqualTo("EXPLAIN");
    assertThat(scoring.markCompleted(fresh, closeAt, closeAt.minusSeconds(300), 30)).isFalse();
    assertThat(column(fresh, "stage")).isEqualTo("CHALLENGE");

    assertThat(scoring.markAbandoned(untouched, closeAt, stale, 30)).isTrue();
    assertThat(column(untouched, "stage")).isEqualTo("ABANDONED");
    assertThat(sessions.find(untouched).orElseThrow().endedAt()).isEqualTo(closeAt);
    assertThat(scoring.markCompleted(untouched, closeAt, stale, 30)).isFalse();
    assertThat(column(untouched, "stage")).isEqualTo("ABANDONED");
  }

  @Test
  void closeWithoutObservedSkipsTheIdleGuardButNeverReopensTerminalSessions() {
    UUID running = raw(UUID.randomUUID(), "EXPLAIN", now(), now());
    UUID done = raw(UUID.randomUUID(), "COMPLETED", now(), now());
    Instant closeAt = now();

    assertThat(scoring.markCompleted(running, closeAt, null, 30)).isTrue();
    assertThat(scoring.markCompleted(done, closeAt, null, 30)).isFalse();
    assertThat(scoring.markAbandoned(running, closeAt, null, 30)).isFalse();
    assertThat(column(running, "stage")).isEqualTo("COMPLETED");
  }

  @Test
  void idleSessionQueriesIgnoreFreshAndTerminalAndOrderOldestFirst() {
    UUID user = UUID.randomUUID();
    Instant base = now().minus(5, ChronoUnit.HOURS);
    UUID older = raw(user, "EXPLAIN", base, base);
    UUID newer = raw(user, "CHALLENGE", base, base.plusSeconds(60));
    raw(user, "SCALE_UP", now(), now());
    raw(user, "COMPLETED", base, base);

    List<IdleSession> idle = scoring.idleSessions(30, 10);

    assertThat(idle).extracting(IdleSession::id).containsExactly(older, newer);
    assertThat(idle.get(0).updatedAt()).isEqualTo(base);
    assertThat(scoring.idleSessions(30, 1)).extracting(IdleSession::id).containsExactly(older);
    assertThat(scoring.idleSession(newer, 30)).map(IdleSession::id).contains(newer);
    assertThat(scoring.idleSession(newer, 600)).isEmpty();
    assertThat(scoring.idleSession(UUID.randomUUID(), 30)).isEmpty();
  }

  @Test
  void rubricIsReadInCriterionEnumOrderNotInsertionOrder() {
    UUID session = UUID.randomUUID();
    rubric(session, RubricCriterion.COMPLEXITY_AWARENESS, "4.5");
    rubric(session, RubricCriterion.CLARITY, "8");
    rubric(session, RubricCriterion.PUSHBACK_HANDLING, "6.25");
    rubric(session, RubricCriterion.TECHNICAL_ACCURACY, "10");

    List<RubricScore> read = scoring.rubric(session);

    assertThat(read)
        .extracting(RubricScore::criterion)
        .containsExactly(
            RubricCriterion.CLARITY,
            RubricCriterion.TECHNICAL_ACCURACY,
            RubricCriterion.PUSHBACK_HANDLING,
            RubricCriterion.COMPLEXITY_AWARENESS);
    assertThat(read.get(2).score()).isEqualByComparingTo("6.25");
    assertThat(scoring.rubric(UUID.randomUUID())).isEmpty();
  }

  @Test
  void rubricRowsRespectTheUniqueCriterionAndScoreRangeConstraints() {
    UUID session = UUID.randomUUID();
    rubric(session, RubricCriterion.CLARITY, "7");

    assertThatThrownBy(() -> rubric(session, RubricCriterion.CLARITY, "9"))
        .isInstanceOf(DuplicateKeyException.class);
    assertThatThrownBy(() -> rubric(session, RubricCriterion.TECHNICAL_ACCURACY, "10.5"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(scoring.rubric(session)).hasSize(1);
  }

  @Test
  void insertRubricGeneratesOneIdPerRowFromTheSupplier() {
    UUID session = UUID.randomUUID();
    List<UUID> minted = new ArrayList<>();

    scoring.insertRubric(
        session,
        List.of(
            new RubricScore(RubricCriterion.CLARITY, new BigDecimal("1"), "a"),
            new RubricScore(RubricCriterion.TECHNICAL_ACCURACY, new BigDecimal("2"), "b")),
        now(),
        () -> {
          UUID id = UUID.randomUUID();
          minted.add(id);
          return id;
        });

    assertThat(jdbc.sql("SELECT id FROM ai.rubric_scores").query(UUID.class).list())
        .containsExactlyInAnyOrderElementsOf(minted);
  }

  @Test
  void completedRubricsJoinOnlyCompletedSessionsFilterByFromAndSortNewestFirst() {
    UUID user = UUID.randomUUID();
    Instant base = now().minus(10, ChronoUnit.DAYS);
    UUID old = raw(user, "COMPLETED", base.minusSeconds(60), base);
    UUID recent =
        raw(user, "COMPLETED", base.plus(5, ChronoUnit.DAYS), base.plus(5, ChronoUnit.DAYS));
    UUID running = raw(user, "EXPLAIN", base, base);
    UUID someoneElse = raw(UUID.randomUUID(), "COMPLETED", base, base);
    for (UUID s : List.of(old, recent, running, someoneElse)) {
      rubric(s, RubricCriterion.CLARITY, "5");
    }
    rubric(recent, RubricCriterion.TECHNICAL_ACCURACY, "7");

    List<RubricRow> all = scoring.completedRubrics(user, null);
    List<RubricRow> since = scoring.completedRubrics(user, base.plus(1, ChronoUnit.DAYS));

    assertThat(all).extracting(RubricRow::sessionId).containsExactlyInAnyOrder(recent, recent, old);
    assertThat(all.get(2).sessionId()).isEqualTo(old);
    assertThat(all.get(0).entryType()).isEqualTo(InterviewEntryType.SUBMISSION);
    assertThat(since).extracting(RubricRow::sessionId).containsOnly(recent);
    assertThat(since).hasSize(2);
  }
}
