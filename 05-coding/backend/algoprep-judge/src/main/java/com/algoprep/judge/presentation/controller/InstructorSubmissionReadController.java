package com.algoprep.judge.presentation.controller;

import com.algoprep.judge.application.dto.SubmissionReads.ClassHistory;
import com.algoprep.judge.application.dto.SubmissionReads.Filters;
import com.algoprep.judge.application.dto.SubmissionReads.InstructorDetail;
import com.algoprep.judge.application.dto.SubmissionReads.InstructorItem;
import com.algoprep.judge.application.dto.SubmissionReads.Page;
import com.algoprep.judge.application.dto.SubmissionReads.Problem;
import com.algoprep.judge.application.dto.SubmissionReads.TestcaseDetail;
import com.algoprep.judge.application.ports.in.GetInstructorSubmissionDetailUseCase;
import com.algoprep.judge.application.ports.in.ListInstructorSubmissionsUseCase;
import com.algoprep.judge.application.ports.in.ListStudentClassSubmissionsUseCase;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/judge/instructor")
@PreAuthorize("hasAuthority('CLASS_MANAGEMENT:READ')")
class InstructorSubmissionReadController {
  private final ListInstructorSubmissionsUseCase list;
  private final GetInstructorSubmissionDetailUseCase detail;
  private final ListStudentClassSubmissionsUseCase history;

  InstructorSubmissionReadController(
      ListInstructorSubmissionsUseCase list,
      GetInstructorSubmissionDetailUseCase detail,
      ListStudentClassSubmissionsUseCase history) {
    this.list = list;
    this.detail = detail;
    this.history = history;
  }

  @GetMapping("/submissions")
  Page<InstructorItem> list(
      JwtAuthenticationToken caller,
      @RequestParam(required = false) String classId,
      @RequestParam(required = false) String result,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String page) {
    UUID selected = classId == null ? null : SubmissionReadParameters.id(classId, "JDG-111");
    return list.list(owner(caller), new Filters(q, result, null, page, selected));
  }

  @GetMapping("/submissions/{submissionId}")
  InstructorResponse detail(JwtAuthenticationToken caller, @PathVariable String submissionId) {
    return InstructorResponse.of(
        detail.detail(owner(caller), SubmissionReadParameters.id(submissionId, "JDG-110")));
  }

  @GetMapping("/classes/{classId}/students/{studentId}/submissions")
  Page<ClassHistory> history(
      JwtAuthenticationToken caller,
      @PathVariable String classId,
      @PathVariable String studentId,
      @RequestParam(required = false) String page) {
    return history.classHistory(
        owner(caller),
        SubmissionReadParameters.id(classId, "JDG-111"),
        SubmissionReadParameters.id(studentId, "JDG-112"),
        page);
  }

  private static UUID owner(JwtAuthenticationToken caller) {
    return UUID.fromString(caller.getToken().getSubject());
  }

  record InstructorResponse(
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
      TestcaseDetail testcaseDetail,
      UUID studentId,
      String studentName,
      UUID classId,
      String className,
      boolean leftClass) {
    static InstructorResponse of(InstructorDetail result) {
      var d = result.detail();
      var m = result.member();
      return new InstructorResponse(
          d.submissionId(),
          d.status(),
          d.language(),
          d.submissionModel(),
          d.submittedAt(),
          d.finishedAt(),
          d.problem(),
          d.sourceCode(),
          d.compileErrorMessage(),
          d.passedCount(),
          d.totalCount(),
          d.scoreRatio(),
          d.runtimeMs(),
          d.memoryKb(),
          d.testcaseDetail(),
          m.studentId(),
          m.studentName(),
          m.classId(),
          m.className(),
          m.leftAt() != null);
    }

    @Override
    public String toString() {
      return "InstructorSubmissionResponse[redacted]";
    }
  }
}
