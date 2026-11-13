package com.algoprep.aireview.presentation.controller;

import com.algoprep.aireview.application.dto.OpenInterviewSessionCommand;
import com.algoprep.aireview.domain.exception.AiInvalidRequestException;
import com.algoprep.aireview.domain.exception.AiSubmissionNotFoundException;
import com.algoprep.aireview.domain.model.CallerCategory;
import com.algoprep.aireview.domain.model.InterviewEntryType;
import com.algoprep.aireview.domain.model.InterviewerLevel;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Validation of endpoint 8 (DD validation F5 section 4, AIR-V03 to AIR-V07). The fields that apply
 * depend on {@code entryType}; for SUBMISSION the three F5-28 settings are ignored and stored as
 * NULL.
 */
final class InterviewRequests {
  static final int QUESTION_REF_LENGTH = 36;
  static final int QUESTION_TEXT_MAX = 2000;
  static final int TOPICS_MAX = 10;
  static final int TOPIC_MAX_LENGTH = 60;
  static final Set<Integer> MAX_TURNS = Set.of(8, 12, 16);

  /** Body of {@code POST /interview-sessions}. */
  record StartRequest(
      String entryType,
      String submissionId,
      String questionRef,
      String questionText,
      List<String> topics,
      String interviewerLevel,
      Integer maxTurns,
      Boolean hintAllowed) {}

  private InterviewRequests() {}

  static OpenInterviewSessionCommand toCommand(
      StartRequest body, java.util.UUID callerId, CallerCategory category) {
    if (body == null) {
      throw v03("request body is required");
    }
    InterviewEntryType type = entryType(body.entryType());
    switch (type) {
      case SUBMISSION -> {
        require(body.submissionId() != null, "submissionId is required");
        forbid(body.questionRef() == null, "questionRef");
        forbid(body.questionText() == null, "questionText");
        forbid(body.topics() == null, "topics");
        return new OpenInterviewSessionCommand(
            callerId,
            category,
            type,
            submissionId(body.submissionId()),
            null,
            null,
            List.of(),
            null,
            null,
            null);
      }
      case QUESTION_BANK -> {
        require(body.questionRef() != null && body.questionText() != null, "question is required");
        forbid(body.submissionId() == null, "submissionId");
        forbid(body.topics() == null, "topics");
        return practice(body, callerId, category, type, List.of());
      }
      default -> {
        require(body.topics() != null, "topics is required");
        forbid(body.submissionId() == null, "submissionId");
        forbid(body.questionRef() == null, "questionRef");
        forbid(body.questionText() == null, "questionText");
        return practice(body, callerId, category, type, topics(body.topics()));
      }
    }
  }

  private static OpenInterviewSessionCommand practice(
      StartRequest body,
      UUID callerId,
      CallerCategory category,
      InterviewEntryType type,
      List<String> topics) {
    boolean bank = type == InterviewEntryType.QUESTION_BANK;
    String ref = bank ? questionRef(body.questionRef()) : null;
    String text = bank ? questionText(body.questionText()) : null;
    InterviewerLevel level = level(body.interviewerLevel());
    int turns = maxTurns(body.maxTurns());
    require(body.hintAllowed() != null, "hintAllowed is required");
    return new OpenInterviewSessionCommand(
        callerId, category, type, null, ref, text, topics, level, turns, body.hintAllowed());
  }

  private static InterviewEntryType entryType(String raw) {
    if (raw == null) {
      throw v03("entryType is required");
    }
    try {
      return InterviewEntryType.valueOf(raw);
    } catch (IllegalArgumentException unknown) {
      throw v03("entryType must be SUBMISSION, QUESTION_BANK or TOPIC_SELECTION");
    }
  }

  private static UUID submissionId(String raw) {
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException invalid) {
      // Same as the Solution Review path: an id that is not a UUID names no submission.
      throw new AiSubmissionNotFoundException();
    }
  }

  private static String questionRef(String raw) {
    if (raw.length() != QUESTION_REF_LENGTH) {
      throw new AiInvalidRequestException("AIR-V07", "questionRef must be a 36 character UUID");
    }
    try {
      UUID.fromString(raw);
    } catch (IllegalArgumentException invalid) {
      throw new AiInvalidRequestException("AIR-V07", "questionRef must be a UUID");
    }
    return raw;
  }

  private static String questionText(String raw) {
    String text = raw.strip();
    if (text.isEmpty() || text.length() > QUESTION_TEXT_MAX) {
      throw new AiInvalidRequestException(
          "AIR-V07", "questionText must have 1 to " + QUESTION_TEXT_MAX + " characters");
    }
    return text;
  }

  /** Trimmed, 1 to 60 characters each, duplicates (ignoring case) merged, 1 to 10 kept. */
  private static List<String> topics(List<String> raw) {
    Map<String, String> unique = new LinkedHashMap<>();
    for (String topic : raw) {
      String text = topic == null ? "" : topic.strip();
      if (text.isEmpty() || text.length() > TOPIC_MAX_LENGTH) {
        throw new AiInvalidRequestException(
            "AIR-V06", "each topic must have 1 to " + TOPIC_MAX_LENGTH + " characters");
      }
      unique.putIfAbsent(text.toLowerCase(Locale.ROOT), text);
    }
    if (raw.isEmpty() || raw.size() > TOPICS_MAX) {
      throw new AiInvalidRequestException("AIR-V06", "topics must have 1 to 10 items");
    }
    return List.copyOf(unique.values());
  }

  private static InterviewerLevel level(String raw) {
    try {
      if (raw == null) {
        throw new IllegalArgumentException("missing");
      }
      return InterviewerLevel.valueOf(raw);
    } catch (IllegalArgumentException invalid) {
      throw new AiInvalidRequestException(
          "AIR-V04", "interviewerLevel must be INTERN, JUNIOR, MIDDLE or SENIOR");
    }
  }

  private static int maxTurns(Integer raw) {
    if (raw == null || !MAX_TURNS.contains(raw)) {
      throw new AiInvalidRequestException("AIR-V05", "maxTurns must be 8, 12 or 16");
    }
    return raw;
  }

  private static void require(boolean ok, String message) {
    if (!ok) {
      throw v03(message);
    }
  }

  private static void forbid(boolean absent, String field) {
    if (!absent) {
      throw v03(field + " is not allowed for this entryType");
    }
  }

  private static AiInvalidRequestException v03(String message) {
    return new AiInvalidRequestException("AIR-V03", message);
  }
}
