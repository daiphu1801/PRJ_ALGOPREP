package com.algoprep.aireview.application.command;

import com.algoprep.aireview.domain.model.AiOutboxEvent;
import com.algoprep.aireview.domain.model.InterviewResult;
import com.algoprep.aireview.domain.model.InterviewSession;
import com.algoprep.aireview.domain.model.RubricCriterion;
import com.algoprep.aireview.domain.model.RubricScore;
import com.algoprep.aireview.domain.ports.out.AiOutboxRepository;
import com.algoprep.aireview.domain.ports.out.InterviewScoringRepository;
import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * The one transaction of BR-AI-09 step 6: close the session, write the four rubric rows and the
 * {@code MockInterviewFinished} outbox row together. The conditional UPDATE runs first, so a lost
 * race writes nothing at all. The LLM call never runs inside it.
 */
@Service
class InterviewScoreStore {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final InterviewScoringRepository scoring;
  private final AiOutboxRepository outbox;
  private final IdGeneratorPort ids;
  private final ClockPort clock;

  InterviewScoreStore(
      InterviewScoringRepository scoring,
      AiOutboxRepository outbox,
      IdGeneratorPort ids,
      ClockPort clock) {
    this.scoring = scoring;
    this.outbox = outbox;
    this.ids = ids;
    this.clock = clock;
  }

  /**
   * Empty when the session was no longer running (or, for the sweep, no longer idle): someone else
   * closed it first and nothing was written.
   *
   * @param observed {@code null} for the learner; the observed row version for the sweep
   */
  @Transactional
  Optional<InterviewResult> commit(
      InterviewSession session, List<RubricScore> scores, Instant observed, int idleMinutes) {
    Instant now = clock.now();
    if (!scoring.markCompleted(session.id(), now, observed, idleMinutes)) {
      return Optional.empty();
    }
    scoring.insertRubric(session.id(), scores, now, ids::newId);
    outbox.insert(
        new AiOutboxEvent(
            ids.newId(),
            AiOutboxEvent.MOCK_INTERVIEW_FINISHED,
            JSON.writeValueAsString(payload(session, scores)),
            now,
            0),
        "InterviewSession",
        session.id());
    return Optional.of(new InterviewResult(session.id(), scores, session.turnCount(), now));
  }

  /** Ids, the score and the four numbers only: no transcript text and no comment. */
  private static Map<String, Object> payload(InterviewSession s, List<RubricScore> scores) {
    Map<String, Object> rubric = new LinkedHashMap<>();
    for (RubricCriterion criterion : RubricCriterion.values()) {
      scores.stream()
          .filter(one -> one.criterion() == criterion)
          .findFirst()
          .ifPresent(one -> rubric.put(criterion.name(), one.score()));
    }
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("sessionId", s.id());
    payload.put("userId", s.userId());
    payload.put("entryType", s.entryType().name());
    payload.put("submissionId", s.submissionId());
    payload.put("overallScore", InterviewResult.overall(scores));
    payload.put("rubric", rubric);
    return payload;
  }
}
