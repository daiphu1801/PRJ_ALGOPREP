package com.algoprep.aireview.application.command;

import com.algoprep.aireview.domain.model.InterviewSession;
import com.algoprep.aireview.domain.model.InterviewTurn;
import com.algoprep.aireview.domain.ports.out.InterviewSessionRepository;
import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The one transactional write of BR-AI-06 step 6; the LLM call never runs inside it. */
@Service
class InterviewSessionStore {
  private final InterviewSessionRepository sessions;
  private final IdGeneratorPort ids;
  private final ClockPort clock;

  InterviewSessionStore(InterviewSessionRepository sessions, IdGeneratorPort ids, ClockPort clock) {
    this.sessions = sessions;
    this.ids = ids;
    this.clock = clock;
  }

  UUID newId() {
    return ids.newId();
  }

  Instant now() {
    return clock.now();
  }

  @Transactional
  void saveOpened(InterviewSession session, InterviewTurn opening) {
    sessions.insertOpened(session, opening);
  }
}
