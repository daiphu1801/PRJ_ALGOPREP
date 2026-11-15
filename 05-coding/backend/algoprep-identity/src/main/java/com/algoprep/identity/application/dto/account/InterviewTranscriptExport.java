package com.algoprep.identity.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The caller's own interview conversations as the interview module lists them, oldest session
 * first. Text is user data and goes into the file untouched; there is no rubric.
 */
public record InterviewTranscriptExport(List<Session> sessions) {
  public InterviewTranscriptExport {
    sessions = List.copyOf(sessions);
  }

  /** One interview session; {@code submissionId} and {@code questionRef} depend on the entry. */
  public record Session(
      UUID sessionId,
      String entryType,
      String stage,
      Instant startedAt,
      Instant endedAt,
      List<String> topics,
      UUID submissionId,
      String questionRef,
      String interviewerLevel,
      List<Turn> turns) {
    public Session {
      topics = List.copyOf(topics);
      turns = List.copyOf(turns);
    }
  }

  /** One line of the conversation. */
  public record Turn(int turnIndex, String role, String stage, String content, Instant createdAt) {}
}
