package com.algoprep.aireview.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.application.ports.out.InterviewReplyTracker;
import com.algoprep.aireview.domain.exception.AiNotOwnerException;
import com.algoprep.aireview.domain.exception.AiSessionNotFoundException;
import com.algoprep.aireview.domain.model.InterviewEntryType;
import com.algoprep.aireview.domain.model.InterviewSession;
import com.algoprep.aireview.domain.model.InterviewSessionStage;
import com.algoprep.aireview.domain.model.InterviewStage;
import com.algoprep.aireview.domain.model.InterviewTurn;
import com.algoprep.aireview.domain.model.ReplyState;
import com.algoprep.aireview.domain.ports.out.InterviewScoringRepository;
import com.algoprep.aireview.domain.ports.out.InterviewSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetInterviewSessionQueryHandlerTest {
  private final InterviewSessionRepository sessions = mock(InterviewSessionRepository.class);
  private final InterviewReplyTracker tracker = mock(InterviewReplyTracker.class);
  private final InterviewScoringRepository scoring = mock(InterviewScoringRepository.class);
  private final GetInterviewSessionQueryHandler handler =
      new GetInterviewSessionQueryHandler(sessions, tracker, scoring);
  private final UUID owner = UUID.randomUUID();

  private InterviewSession session(InterviewSessionStage stage) {
    return new InterviewSession(
        UUID.randomUUID(),
        owner,
        InterviewEntryType.SUBMISSION,
        UUID.randomUUID(),
        null,
        null,
        null,
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

  private InterviewTurn turn(UUID session, int index, InterviewTurn.Role role) {
    return new InterviewTurn(
        UUID.randomUUID(), session, index, role, InterviewStage.EXPLAIN, "x", Instant.EPOCH);
  }

  @Test
  void ownerReadsTurnsInOrderWithIdleReplyState() {
    InterviewSession s = session(InterviewSessionStage.EXPLAIN);
    when(sessions.find(s.id())).thenReturn(Optional.of(s));
    when(sessions.turns(s.id())).thenReturn(List.of(turn(s.id(), 1, InterviewTurn.Role.AI)));

    var detail = handler.get(owner, s.id());

    assertThat(detail.turns()).hasSize(1);
    assertThat(detail.replyState()).isEqualTo(ReplyState.IDLE);
  }

  @Test
  void lastUserLineMeansFailedUnlessAReplyIsRunning() {
    InterviewSession s = session(InterviewSessionStage.CHALLENGE);
    when(sessions.find(s.id())).thenReturn(Optional.of(s));
    when(sessions.turns(s.id()))
        .thenReturn(
            List.of(
                turn(s.id(), 1, InterviewTurn.Role.AI), turn(s.id(), 2, InterviewTurn.Role.USER)));
    assertThat(handler.get(owner, s.id()).replyState()).isEqualTo(ReplyState.FAILED);

    when(tracker.isGenerating(s.id())).thenReturn(true);
    assertThat(handler.get(owner, s.id()).replyState()).isEqualTo(ReplyState.GENERATING);
  }

  @Test
  void endedSessionIsIdleEvenWithADanglingUserLine() {
    InterviewSession s = session(InterviewSessionStage.ABANDONED);
    when(sessions.find(s.id())).thenReturn(Optional.of(s));
    when(sessions.turns(s.id())).thenReturn(List.of(turn(s.id(), 1, InterviewTurn.Role.USER)));
    assertThat(handler.get(owner, s.id()).replyState()).isEqualTo(ReplyState.IDLE);
  }

  @Test
  void unknownIs110AndForeignIs302() {
    UUID missing = UUID.randomUUID();
    when(sessions.find(missing)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> handler.get(owner, missing))
        .isInstanceOf(AiSessionNotFoundException.class);

    InterviewSession s = session(InterviewSessionStage.EXPLAIN);
    when(sessions.find(s.id())).thenReturn(Optional.of(s));
    assertThatThrownBy(() -> handler.get(UUID.randomUUID(), s.id()))
        .isInstanceOf(AiNotOwnerException.class);
  }
}
