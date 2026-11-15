package com.algoprep.aireview.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.aireview.domain.model.IdleSession;
import com.algoprep.aireview.domain.model.InterviewEntryType;
import com.algoprep.aireview.domain.model.InterviewSession;
import com.algoprep.aireview.domain.model.InterviewSessionStage;
import com.algoprep.aireview.domain.model.InterviewStage;
import com.algoprep.aireview.domain.model.InterviewTurn;
import com.algoprep.aireview.domain.model.InterviewerLevel;
import com.algoprep.aireview.domain.model.RubricCriterion;
import com.algoprep.aireview.domain.model.RubricScore;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Interview session, turn and scoring repositories: SQL text, binding and row mapping. */
class JdbcInterviewRepositoriesTest {
  private static final Instant T0 = Instant.parse("2026-11-01T10:00:00Z");
  private static final OffsetDateTime T0_UTC = OffsetDateTime.ofInstant(T0, ZoneOffset.UTC);
  private static final UUID SID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
  private static final UUID UID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

  private static Map<String, Object> sessionRow(String entry, Object... settings) {
    Object topics = settings[0];
    Object level = settings[1];
    Object maxTurns = settings[2];
    Object hint = settings[3];
    Object ended = settings[4];
    return FakeJdbc.row0(
        "id", SID,
        "user_id", UID,
        "entry_type", entry,
        "submission_id", null,
        "question_ref", "Q1",
        "question_text", "Explain hashing",
        "topics", topics,
        "interviewer_level", level,
        "max_turns", maxTurns,
        "hint_allowed", hint,
        "hint_count", 1,
        "stage", "CHALLENGE",
        "turn_count", 3,
        "started_at", T0_UTC,
        "ended_at", ended,
        "updated_at", T0_UTC);
  }

  private static Map<String, Object> turnRow() {
    return FakeJdbc.row0(
        "id",
        UUID.randomUUID(),
        "interview_session_id",
        SID,
        "turn_index",
        2,
        "role",
        "AI",
        "stage_at_turn",
        "EXPLAIN",
        "content",
        "Why?",
        "created_at",
        T0_UTC);
  }

  private static InterviewSession topicSession() {
    return new InterviewSession(
        SID,
        UID,
        InterviewEntryType.TOPIC_SELECTION,
        null,
        null,
        "Hashing",
        List.of("hash", "tree"),
        InterviewerLevel.JUNIOR,
        8,
        true,
        0,
        InterviewSessionStage.EXPLAIN,
        0,
        T0,
        null,
        T0);
  }

  @Test
  void insertOpenedWritesSessionThenOpeningTurnWithTopicsOnlyForTopicEntry() {
    var jdbc = new FakeJdbc();
    var repo = new JdbcInterviewSessionRepository(jdbc.provider());
    var opening =
        new InterviewTurn(
            UUID.randomUUID(), SID, 1, InterviewTurn.Role.AI, InterviewStage.EXPLAIN, "Hello", T0);

    repo.insertOpened(topicSession(), opening);

    assertThat(jdbc.calls).hasSize(2);
    assertThat(jdbc.calls.get(0).sql()).contains("INSERT INTO ai.interview_sessions");
    assertThat(jdbc.calls.get(0).params().values()).contains("[\"hash\",\"tree\"]", "JUNIOR", 8);
    assertThat(jdbc.calls.get(1).sql()).contains("INSERT INTO ai.interview_turns");
    assertThat(jdbc.calls.get(1).params()).containsEntry(4, "AI").containsEntry(6, "Hello");

    var jdbc2 = new FakeJdbc();
    var sub =
        new InterviewSession(
            SID,
            UID,
            InterviewEntryType.SUBMISSION,
            UUID.randomUUID(),
            null,
            "q",
            List.of(),
            null,
            null,
            null,
            0,
            InterviewSessionStage.EXPLAIN,
            0,
            T0,
            null,
            T0);
    new JdbcInterviewSessionRepository(jdbc2.provider()).insertOpened(sub, opening);
    assertThat(jdbc2.calls.get(0).params())
        .containsEntry(5, null)
        .containsEntry(7, null)
        .containsEntry(8, null);
  }

  @Test
  void findMapsNullableSettingsAndTopics() {
    var jdbc = new FakeJdbc();
    jdbc.rows(List.of(sessionRow("TOPIC_SELECTION", "[\"a\",\"b\"]", "SENIOR", 6, true, null)));
    jdbc.rows(List.of(sessionRow("SUBMISSION", null, null, null, null, T0_UTC)));
    jdbc.none();
    var repo = new JdbcInterviewSessionRepository(jdbc.provider());

    InterviewSession topic = repo.find(SID).orElseThrow();
    InterviewSession sub = repo.find(SID).orElseThrow();

    assertThat(topic.topics()).containsExactly("a", "b");
    assertThat(topic.interviewerLevel()).isEqualTo(InterviewerLevel.SENIOR);
    assertThat(topic.maxTurns()).isEqualTo(6);
    assertThat(topic.hintAllowed()).isTrue();
    assertThat(topic.endedAt()).isNull();
    assertThat(sub.topics()).isEmpty();
    assertThat(sub.interviewerLevel()).isNull();
    assertThat(sub.maxTurns()).isNull();
    assertThat(sub.hintAllowed()).isNull();
    assertThat(sub.endedAt()).isEqualTo(T0);
    assertThat(repo.find(SID)).isEmpty();
  }

  @Test
  void listingQueriesAreScopedByOwnerAndSession() {
    var jdbc = new FakeJdbc();
    jdbc.rows(List.of(sessionRow("QUESTION_BANK", null, "INTERN", 5, false, null)));
    jdbc.rows(List.of(turnRow()));
    jdbc.rows(List.of(turnRow(), turnRow()));
    var repo = new JdbcInterviewSessionRepository(jdbc.provider());

    assertThat(repo.findAllByUser(UID)).hasSize(1);
    assertThat(jdbc.lastParams()).containsEntry(1, UID);
    assertThat(repo.turns(SID))
        .singleElement()
        .satisfies(t -> assertThat(t.turnIndex()).isEqualTo(2));
    assertThat(jdbc.lastSql()).contains("ORDER BY turn_index");
    assertThat(repo.turnsOfUser(UID)).hasSize(2);
    assertThat(jdbc.lastSql()).contains("JOIN ai.interview_sessions");
  }

  @Test
  void countUserTurnsAndHintIncrement() {
    var jdbc = new FakeJdbc();
    jdbc.row("count", 4);
    jdbc.update(1).update(0);
    var repo = new JdbcInterviewSessionRepository(jdbc.provider());

    assertThat(repo.countUserTurns(SID, InterviewStage.CHALLENGE)).isEqualTo(4);
    assertThat(jdbc.lastParams()).containsEntry(2, "CHALLENGE");
    assertThat(repo.incrementHintCount(SID, 3)).isTrue();
    assertThat(repo.incrementHintCount(SID, 3)).isFalse();
    assertThat(jdbc.lastParams()).containsEntry(2, 3);
  }

  @Test
  void turnRepositoryInsertsUpdatesTouchesAndAdvances() {
    var jdbc = new FakeJdbc();
    jdbc.update(1).update(1).update(1).update(1).update(0);
    var repo = new JdbcInterviewTurnRepository(jdbc.provider());

    repo.insertTurn(
        new InterviewTurn(
            UUID.randomUUID(),
            SID,
            3,
            InterviewTurn.Role.USER,
            InterviewStage.SCALE_UP,
            "ans",
            T0));
    assertThat(jdbc.lastParams()).containsEntry(3, 3).containsEntry(4, "USER");
    assertThat(jdbc.lastParams()).containsEntry(5, "SCALE_UP").containsEntry(7, T0_UTC);

    repo.updateUserContent(SID, 3, "edited");
    assertThat(jdbc.lastSql()).contains("role = 'USER'");
    assertThat(jdbc.lastParams()).containsEntry(1, "edited");

    repo.touch(SID, T0);
    assertThat(jdbc.lastParams()).containsEntry(1, T0_UTC).containsEntry(2, SID);

    assertThat(repo.advance(SID, InterviewSessionStage.CHALLENGE, T0)).isTrue();
    assertThat(jdbc.lastParams()).containsEntry(1, "CHALLENGE");
    assertThat(repo.advance(SID, InterviewSessionStage.CHALLENGE, T0)).isFalse();
  }

  @Test
  void rubricIsSortedByCriterionOrder() {
    var jdbc = new FakeJdbc();
    jdbc.rows(
        List.of(
            FakeJdbc.row0("criterion_code", "COMPLEXITY_AWARENESS", "score", 5, "comment", "c4"),
            FakeJdbc.row0(
                "criterion_code", "CLARITY", "score", new BigDecimal("8.5"), "comment", "c1")));
    var repo = new JdbcInterviewScoringRepository(jdbc.provider());

    List<RubricScore> rubric = repo.rubric(SID);

    assertThat(rubric)
        .extracting(RubricScore::criterion)
        .containsExactly(RubricCriterion.CLARITY, RubricCriterion.COMPLEXITY_AWARENESS);
    assertThat(rubric.get(0).score()).isEqualByComparingTo("8.5");
  }

  @Test
  void closeWithoutObservedSkipsIdleGuardAndWithObservedAddsIt() {
    var jdbc = new FakeJdbc();
    jdbc.update(1).update(0).update(1);
    var repo = new JdbcInterviewScoringRepository(jdbc.provider());

    assertThat(repo.markCompleted(SID, T0, null, 0)).isTrue();
    assertThat(jdbc.lastSql()).contains("stage = 'COMPLETED'").doesNotContain("make_interval");

    assertThat(repo.markAbandoned(SID, T0, T0, 30)).isFalse();
    assertThat(jdbc.lastSql()).contains("stage = 'ABANDONED'").contains("make_interval");
    assertThat(jdbc.lastParams()).containsEntry(4, T0_UTC).containsEntry(5, 30);

    assertThat(repo.markCompleted(SID, T0, T0, 30)).isTrue();
  }

  @Test
  void insertRubricWritesOneRowPerScoreWithGeneratedIds() {
    var jdbc = new FakeJdbc();
    var repo = new JdbcInterviewScoringRepository(jdbc.provider());
    List<UUID> generated = new ArrayList<>();

    repo.insertRubric(
        SID,
        List.of(
            new RubricScore(RubricCriterion.CLARITY, new BigDecimal("7.0"), "ok"),
            new RubricScore(RubricCriterion.TECHNICAL_ACCURACY, new BigDecimal("6.0"), "meh")),
        T0,
        () -> {
          UUID id = UUID.randomUUID();
          generated.add(id);
          return id;
        });

    assertThat(jdbc.calls).hasSize(2);
    assertThat(generated).hasSize(2);
    assertThat(jdbc.calls.get(1).params())
        .containsEntry(1, generated.get(1))
        .containsEntry(3, "TECHNICAL_ACCURACY")
        .containsEntry(5, "meh");
    assertThat(jdbc.lastSql()).contains("'MOCK_INTERVIEW'");
  }

  @Test
  void idleSessionQueriesMapRows() {
    var jdbc = new FakeJdbc();
    var row = FakeJdbc.row0("id", SID, "turn_count", 4, "updated_at", T0_UTC);
    jdbc.rows(List.of(row));
    jdbc.rows(List.of(row));
    jdbc.none();
    var repo = new JdbcInterviewScoringRepository(jdbc.provider());

    assertThat(repo.idleSessions(45, 20)).containsExactly(new IdleSession(SID, 4, T0));
    assertThat(jdbc.lastParams()).containsEntry(1, 45).containsEntry(2, 20);
    assertThat(repo.idleSession(SID, 45)).contains(new IdleSession(SID, 4, T0));
    assertThat(repo.idleSession(SID, 45)).isEmpty();
  }

  @Test
  void completedRubricsBindsNullFromAsNull() {
    var jdbc = new FakeJdbc();
    jdbc.rows(
        List.of(
            FakeJdbc.row0(
                "id",
                SID,
                "entry_type",
                "SUBMISSION",
                "started_at",
                T0_UTC,
                "ended_at",
                T0_UTC.plusMinutes(10),
                "criterion_code",
                "CLARITY",
                "score",
                new BigDecimal("9.0"))));
    var repo = new JdbcInterviewScoringRepository(jdbc.provider());

    var rows = repo.completedRubrics(UID, null);

    assertThat(rows)
        .singleElement()
        .satisfies(r -> assertThat(r.criterion()).isEqualTo(RubricCriterion.CLARITY));
    assertThat(rows.get(0).endedAt()).isEqualTo(T0.plusSeconds(600));
    assertThat(jdbc.lastParams()).containsEntry(2, null);

    repo.completedRubrics(UID, T0);
    assertThat(jdbc.lastParams()).containsEntry(2, T0_UTC);
  }
}
