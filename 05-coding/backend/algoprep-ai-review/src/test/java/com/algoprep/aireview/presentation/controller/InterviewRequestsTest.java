package com.algoprep.aireview.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.aireview.application.dto.OpenInterviewSessionCommand;
import com.algoprep.aireview.domain.exception.AiInvalidRequestException;
import com.algoprep.aireview.domain.exception.AiSubmissionNotFoundException;
import com.algoprep.aireview.domain.model.CallerCategory;
import com.algoprep.aireview.presentation.controller.InterviewRequests.StartRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class InterviewRequestsTest {
  private final UUID user = UUID.randomUUID();
  private final String ref = UUID.randomUUID().toString();

  private OpenInterviewSessionCommand convert(StartRequest body) {
    return InterviewRequests.toCommand(body, user, CallerCategory.STUDENT);
  }

  private void rejects(StartRequest body, String code) {
    assertThatThrownBy(() -> convert(body))
        .isInstanceOfSatisfying(
            AiInvalidRequestException.class, e -> assertThat(e.errorCode()).isEqualTo(code));
  }

  private StartRequest topics(List<String> topics) {
    return new StartRequest("TOPIC_SELECTION", null, null, null, topics, "JUNIOR", 8, true);
  }

  @Test
  void submissionIgnoresPracticeSettingsAndStoresThemNull() {
    UUID submission = UUID.randomUUID();
    var c =
        convert(
            new StartRequest(
                "SUBMISSION", submission.toString(), null, null, null, "SENIOR", 16, true));
    assertThat(c.submissionId()).isEqualTo(submission);
    assertThat(c.maxTurns()).isNull();
    assertThat(c.interviewerLevel()).isNull();
    assertThat(c.hintAllowed()).isNull();
  }

  @Test
  void fieldSetMustMatchTheEntryType() {
    rejects(null, "AIR-V03");
    rejects(new StartRequest(null, null, null, null, null, null, null, null), "AIR-V03");
    rejects(new StartRequest("NOPE", null, null, null, null, null, null, null), "AIR-V03");
    rejects(new StartRequest("SUBMISSION", null, null, null, null, null, null, null), "AIR-V03");
    rejects(
        new StartRequest(
            "SUBMISSION", UUID.randomUUID().toString(), null, "q", null, null, null, null),
        "AIR-V03");
    rejects(new StartRequest("QUESTION_BANK", null, ref, null, null, "JUNIOR", 8, true), "AIR-V03");
    rejects(
        new StartRequest("QUESTION_BANK", null, ref, "q", List.of("a"), "JUNIOR", 8, true),
        "AIR-V03");
    rejects(
        new StartRequest("TOPIC_SELECTION", null, null, null, null, "JUNIOR", 8, true), "AIR-V03");
    rejects(
        new StartRequest("TOPIC_SELECTION", null, ref, null, List.of("a"), "JUNIOR", 8, true),
        "AIR-V03");
    rejects(
        new StartRequest("TOPIC_SELECTION", null, null, null, List.of("a"), "JUNIOR", 8, null),
        "AIR-V03");
  }

  @Test
  void nonUuidSubmissionIsUnknownSubmission() {
    assertThatThrownBy(
            () ->
                convert(new StartRequest("SUBMISSION", "abc", null, null, null, null, null, null)))
        .isInstanceOf(AiSubmissionNotFoundException.class);
  }

  @Test
  void maxTurnsAndLevelBoundaries() {
    for (int ok : new int[] {8, 12, 16}) {
      assertThat(
              convert(
                      new StartRequest(
                          "TOPIC_SELECTION", null, null, null, List.of("a"), "INTERN", ok, false))
                  .maxTurns())
          .isEqualTo(ok);
    }
    rejects(
        new StartRequest("TOPIC_SELECTION", null, null, null, List.of("a"), "INTERN", 10, false),
        "AIR-V05");
    rejects(
        new StartRequest("TOPIC_SELECTION", null, null, null, List.of("a"), "INTERN", null, false),
        "AIR-V05");
    rejects(
        new StartRequest("TOPIC_SELECTION", null, null, null, List.of("a"), "BOSS", 8, false),
        "AIR-V04");
    rejects(
        new StartRequest("TOPIC_SELECTION", null, null, null, List.of("a"), null, 8, false),
        "AIR-V04");
  }

  @Test
  void questionRefAndTextBoundaries() {
    rejects(
        new StartRequest("QUESTION_BANK", null, "IQ-001", "q", null, "JUNIOR", 8, true), "AIR-V07");
    rejects(
        new StartRequest("QUESTION_BANK", null, "z".repeat(36), "q", null, "JUNIOR", 8, true),
        "AIR-V07");
    rejects(
        new StartRequest("QUESTION_BANK", null, ref, "   ", null, "JUNIOR", 8, true), "AIR-V07");
    rejects(
        new StartRequest("QUESTION_BANK", null, ref, "x".repeat(2001), null, "JUNIOR", 8, true),
        "AIR-V07");
    var edge =
        convert(
            new StartRequest(
                "QUESTION_BANK",
                null,
                ref,
                "  " + "x".repeat(2000) + " ",
                null,
                "JUNIOR",
                8,
                true));
    assertThat(edge.questionText()).hasSize(2000);
  }

  @Test
  void topicsAreTrimmedDedupedAndBounded() {
    assertThat(convert(topics(List.of(" Graphs ", "graphs", "DP"))).topics())
        .containsExactly("Graphs", "DP");
    assertThat(convert(topics(List.of("x".repeat(60)))).topics()).hasSize(1);
    rejects(topics(List.of("x".repeat(61))), "AIR-V06");
    rejects(topics(List.of(" ")), "AIR-V06");
    rejects(topics(new ArrayList<>(java.util.Arrays.asList("a", null))), "AIR-V06");
    rejects(topics(List.of()), "AIR-V06");
    List<String> ten = IntStream.range(0, 10).mapToObj(i -> "t" + i).toList();
    assertThat(convert(topics(ten)).topics()).hasSize(10);
    rejects(topics(IntStream.range(0, 11).mapToObj(i -> "t" + i).toList()), "AIR-V06");
  }
}
