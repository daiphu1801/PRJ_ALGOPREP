package com.algoprep.aireview.application.dto;

import com.algoprep.aireview.domain.model.CallerCategory;
import com.algoprep.aireview.domain.model.InterviewEntryType;
import com.algoprep.aireview.domain.model.InterviewerLevel;
import java.util.List;
import java.util.UUID;

/**
 * Validated input of endpoint 8. The presentation layer has already enforced which fields apply to
 * which entry type (AIR-V03 to AIR-V07); for SUBMISSION the three F5-28 settings are null.
 */
public record OpenInterviewSessionCommand(
    UUID callerId,
    CallerCategory category,
    InterviewEntryType entryType,
    UUID submissionId,
    String questionRef,
    String questionText,
    List<String> topics,
    InterviewerLevel interviewerLevel,
    Integer maxTurns,
    Boolean hintAllowed) {
  public OpenInterviewSessionCommand {
    topics = topics == null ? List.of() : List.copyOf(topics);
  }
}
