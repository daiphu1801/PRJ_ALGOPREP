package com.algoprep.bootstrap.identity;

import com.algoprep.aireview.application.dto.InterviewTranscript;
import com.algoprep.aireview.application.ports.in.InterviewTranscriptReadUseCase;
import com.algoprep.identity.application.dto.InterviewTranscriptExport;
import com.algoprep.identity.application.ports.out.InterviewTranscriptExportPort;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Type translation only: identity asks ai-review (P4) for every conversation of one user. The read
 * runs under a 5 second deadline; a failure or a missed deadline is one {@link
 * IllegalStateException}, never a partial file. The text is passed through untouched.
 */
@Component("interviewTranscriptExportBridge")
final class InterviewTranscriptExportBridge implements InterviewTranscriptExportPort {
  private final InterviewTranscriptReadUseCase reader;
  private final Duration limit;

  @Autowired
  InterviewTranscriptExportBridge(InterviewTranscriptReadUseCase reader) {
    this(reader, Duration.ofSeconds(5));
  }

  InterviewTranscriptExportBridge(InterviewTranscriptReadUseCase reader, Duration limit) {
    this.reader = reader;
    this.limit = limit;
  }

  @Override
  public InterviewTranscriptExport readAll(UUID userId) {
    return BridgeDeadline.call(
        "interview-transcript-export-bridge",
        limit,
        () ->
            new InterviewTranscriptExport(
                reader.listForUser(userId).stream()
                    .map(InterviewTranscriptExportBridge::session)
                    .toList()));
  }

  private static InterviewTranscriptExport.Session session(InterviewTranscript t) {
    return new InterviewTranscriptExport.Session(
        t.sessionId(),
        t.entryType().name(),
        t.stage().name(),
        t.startedAt(),
        t.endedAt(),
        t.topics(),
        t.submissionId(),
        t.questionRef(),
        t.interviewerLevel() == null ? null : t.interviewerLevel().name(),
        t.turns().stream()
            .map(
                x ->
                    new InterviewTranscriptExport.Turn(
                        x.turnIndex(),
                        x.role().name(),
                        x.stage().name(),
                        x.content(),
                        x.createdAt()))
            .toList());
  }
}
