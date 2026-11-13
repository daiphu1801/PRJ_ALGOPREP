package com.algoprep.aireview.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.application.dto.InterviewTranscript;
import com.algoprep.aireview.domain.model.InterviewEntryType;
import com.algoprep.aireview.domain.model.InterviewSession;
import com.algoprep.aireview.domain.model.InterviewSessionStage;
import com.algoprep.aireview.domain.model.InterviewStage;
import com.algoprep.aireview.domain.model.InterviewTurn;
import com.algoprep.aireview.domain.ports.out.InterviewSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReadInterviewTranscriptsQueryHandlerTest {
  private final InterviewSessionRepository sessions = mock(InterviewSessionRepository.class);
  private final ReadInterviewTranscriptsQueryHandler handler =
      new ReadInterviewTranscriptsQueryHandler(sessions);
  private final UUID user = UUID.randomUUID();

  private InterviewSession session(UUID id, InterviewSessionStage stage) {
    return new InterviewSession(
        id,
        user,
        InterviewEntryType.TOPIC_SELECTION,
        null,
        null,
        null,
        List.of("array"),
        null,
        null,
        null,
        0,
        stage,
        0,
        Instant.EPOCH,
        null,
        Instant.EPOCH);
  }

  private InterviewTurn turn(UUID session, int index, InterviewTurn.Role role, String text) {
    return new InterviewTurn(
        UUID.randomUUID(), session, index, role, InterviewStage.EXPLAIN, text, Instant.EPOCH);
  }

  @Test
  void linesAreGroupedUnderTheirSessionInOrderAndAnAbandonedSessionStaysIn() {
    UUID a = UUID.randomUUID();
    UUID b = UUID.randomUUID();
    when(sessions.findAllByUser(user))
        .thenReturn(
            List.of(
                session(a, InterviewSessionStage.COMPLETED),
                session(b, InterviewSessionStage.ABANDONED)));
    when(sessions.turnsOfUser(user))
        .thenReturn(
            List.of(
                turn(a, 1, InterviewTurn.Role.AI, "q"),
                turn(a, 2, InterviewTurn.Role.USER, "a"),
                turn(b, 1, InterviewTurn.Role.AI, "q2")));

    List<InterviewTranscript> result = handler.listForUser(user);

    assertThat(result).extracting(InterviewTranscript::sessionId).containsExactly(a, b);
    assertThat(result.get(0).turns())
        .extracting(InterviewTranscript.Turn::content)
        .containsExactly("q", "a");
    assertThat(result.get(1).stage()).isEqualTo(InterviewSessionStage.ABANDONED);
    assertThat(result.get(1).turns()).hasSize(1);
    assertThat(result.get(0).topics()).containsExactly("array");
  }

  @Test
  void noSessionsIsAnEmptyList() {
    when(sessions.findAllByUser(user)).thenReturn(List.of());
    when(sessions.turnsOfUser(user)).thenReturn(List.of());

    assertThat(handler.listForUser(user)).isEmpty();
  }

  @Test
  void aSessionWithoutLinesGetsAnEmptyTurnList() {
    UUID a = UUID.randomUUID();
    when(sessions.findAllByUser(user))
        .thenReturn(List.of(session(a, InterviewSessionStage.EXPLAIN)));
    when(sessions.turnsOfUser(user)).thenReturn(List.of());

    assertThat(handler.listForUser(user))
        .singleElement()
        .satisfies(t -> assertThat(t.turns()).isEmpty());
  }
}
