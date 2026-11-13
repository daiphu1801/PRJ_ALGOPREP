package com.algoprep.aireview.application.dto;

import com.algoprep.aireview.domain.model.InterviewEntryType;
import com.algoprep.aireview.domain.model.InterviewSessionStage;
import com.algoprep.aireview.domain.model.InterviewStage;
import com.algoprep.aireview.domain.model.InterviewTurn;
import com.algoprep.aireview.domain.model.InterviewerLevel;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * One interview session of the caller with every line in order (P4). No rubric: the export is the
 * conversation, not the grade.
 */
public record InterviewTranscript(
    UUID sessionId,
    InterviewEntryType entryType,
    InterviewSessionStage stage,
    Instant startedAt,
    Instant endedAt,
    List<String> topics,
    UUID submissionId,
    String questionRef,
    InterviewerLevel interviewerLevel,
    List<Turn> turns) {
  public InterviewTranscript {
    topics = List.copyOf(topics);
    turns = List.copyOf(turns);
  }

  /** One line of the conversation. */
  public record Turn(
      int turnIndex,
      InterviewTurn.Role role,
      InterviewStage stage,
      String content,
      Instant createdAt) {}
}
