package com.algoprep.aireview.application.query;

import com.algoprep.aireview.application.dto.InterviewTranscript;
import com.algoprep.aireview.application.ports.in.InterviewTranscriptReadUseCase;
import com.algoprep.aireview.domain.model.InterviewSession;
import com.algoprep.aireview.domain.model.InterviewTurn;
import com.algoprep.aireview.domain.ports.out.InterviewSessionRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** P4: two reads keyed by the user id (sessions, then their lines), joined in memory. */
@Service
@Transactional(readOnly = true)
class ReadInterviewTranscriptsQueryHandler implements InterviewTranscriptReadUseCase {
  private final InterviewSessionRepository sessions;

  ReadInterviewTranscriptsQueryHandler(InterviewSessionRepository sessions) {
    this.sessions = sessions;
  }

  @Override
  public List<InterviewTranscript> listForUser(UUID userId) {
    Map<UUID, List<InterviewTranscript.Turn>> bySession = new HashMap<>();
    for (InterviewTurn t : sessions.turnsOfUser(userId)) {
      bySession
          .computeIfAbsent(t.sessionId(), id -> new ArrayList<>())
          .add(
              new InterviewTranscript.Turn(
                  t.turnIndex(), t.role(), t.stage(), t.content(), t.createdAt()));
    }
    return sessions.findAllByUser(userId).stream()
        .map(s -> transcript(s, bySession.getOrDefault(s.id(), List.of())))
        .toList();
  }

  private static InterviewTranscript transcript(
      InterviewSession s, List<InterviewTranscript.Turn> turns) {
    return new InterviewTranscript(
        s.id(),
        s.entryType(),
        s.stage(),
        s.startedAt(),
        s.endedAt(),
        s.topics(),
        s.submissionId(),
        s.questionRef(),
        s.interviewerLevel(),
        turns);
  }
}
