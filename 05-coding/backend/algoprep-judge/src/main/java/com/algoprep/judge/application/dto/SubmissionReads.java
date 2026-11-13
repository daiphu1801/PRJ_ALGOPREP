package com.algoprep.judge.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Whitelisted read projections. Source appears only in the authorized detail projection. */
public final class SubmissionReads {
  private SubmissionReads() {}

  public record Problem(
      UUID problemId,
      String code,
      String title,
      String slug,
      String levelDisplayName,
      boolean withdrawn) {}

  public record Membership(
      UUID classId,
      String className,
      UUID studentId,
      String studentName,
      Instant joinedAt,
      Instant leftAt) {}

  public record Window(Membership member, ClassAssignment assignment) {}

  public record Filters(String q, String verdict, String language, String page, UUID classId) {}

  public record Page<T>(List<T> items, int page, int pageSize, long totalCount) {
    public Page {
      items = List.copyOf(items);
    }
  }

  public record Summary(
      UUID submissionId,
      String language,
      String status,
      Integer passedCount,
      Integer totalCount,
      BigDecimal scoreRatio,
      Integer runtimeMs,
      Integer memoryKb,
      Instant submittedAt) {}

  public record History(
      UUID submissionId,
      Instant submittedAt,
      Problem problem,
      String language,
      String status,
      Integer passedCount,
      Integer totalCount,
      Integer runtimeMs) {}

  public record Testcase(
      int order,
      String visibility,
      UUID testcaseId,
      String verdict,
      int runtimeMs,
      int memoryKb,
      String actualOutput,
      String stderrSnippet) {}

  public record TestcaseDetail(boolean available, String unavailableReason, List<Testcase> items) {
    public TestcaseDetail {
      items = List.copyOf(items);
    }
  }

  public record Detail(
      UUID submissionId,
      String status,
      String language,
      String submissionModel,
      Instant submittedAt,
      Instant finishedAt,
      Problem problem,
      String sourceCode,
      String compileErrorMessage,
      Integer passedCount,
      Integer totalCount,
      BigDecimal scoreRatio,
      Integer runtimeMs,
      Integer memoryKb,
      Integer beatsPercent,
      Long beatsSampleSize,
      TestcaseDetail testcaseDetail) {
    @Override
    public String toString() {
      return "SubmissionDetail[redacted]";
    }
  }

  public record InstructorDetail(Detail detail, Membership member) {}

  public record ProgressCase(int order, String verdict) {}

  public record Progress(
      UUID submissionId,
      String status,
      int seq,
      @com.fasterxml.jackson.annotation.JsonProperty("isFinal") boolean isFinal,
      Integer totalCount,
      int completedCount,
      int passedCount,
      List<ProgressCase> testcases,
      BigDecimal scoreRatio,
      Integer firstFailingOrder) {
    public Progress {
      testcases = List.copyOf(testcases);
    }
  }

  public record InstructorItem(
      UUID submissionId,
      UUID studentId,
      String studentName,
      UUID classId,
      String className,
      UUID problemId,
      String problemTitle,
      String language,
      String status,
      Integer passedCount,
      Integer totalCount,
      Instant submittedAt,
      boolean leftClass) {}

  public record ClassHistory(
      UUID submissionId,
      String problemTitle,
      String language,
      String status,
      BigDecimal scoreRatio,
      Instant submittedAt) {}
}
