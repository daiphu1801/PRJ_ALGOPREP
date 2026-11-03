package com.algoprep.judge.presentation.controller;

import com.algoprep.judge.application.dto.SubmissionReads.Detail;
import com.algoprep.judge.application.dto.SubmissionReads.Filters;
import com.algoprep.judge.application.dto.SubmissionReads.History;
import com.algoprep.judge.application.dto.SubmissionReads.Page;
import com.algoprep.judge.application.dto.SubmissionReads.Progress;
import com.algoprep.judge.application.dto.SubmissionReads.Summary;
import com.algoprep.judge.application.ports.in.GetOwnedSubmissionDetailUseCase;
import com.algoprep.judge.application.ports.in.GetSubmissionProgressUseCase;
import com.algoprep.judge.application.ports.in.ListMyProblemSubmissionsUseCase;
import com.algoprep.judge.application.ports.in.ListMySubmissionHistoryUseCase;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/judge")
@PreAuthorize("hasRole('STUDENT')")
class MySubmissionReadController {
  private final GetOwnedSubmissionDetailUseCase details;
  private final GetSubmissionProgressUseCase progress;
  private final ListMyProblemSubmissionsUseCase recent;
  private final ListMySubmissionHistoryUseCase history;

  MySubmissionReadController(
      GetOwnedSubmissionDetailUseCase details,
      GetSubmissionProgressUseCase progress,
      ListMyProblemSubmissionsUseCase recent,
      ListMySubmissionHistoryUseCase history) {
    this.details = details;
    this.progress = progress;
    this.recent = recent;
    this.history = history;
  }

  @GetMapping("/submissions/{submissionId}")
  Detail detail(JwtAuthenticationToken caller, @PathVariable String submissionId) {
    return details.get(owner(caller), SubmissionReadParameters.id(submissionId, "JDG-110"));
  }

  @GetMapping("/submissions/{submissionId}/progress")
  Progress progress(JwtAuthenticationToken caller, @PathVariable String submissionId) {
    return progress.getProgress(
        owner(caller), SubmissionReadParameters.id(submissionId, "JDG-110"));
  }

  @GetMapping("/problems/{problemId}/submissions/mine")
  RecentResponse recent(
      JwtAuthenticationToken caller,
      @PathVariable String problemId,
      @RequestParam(required = false) String limit) {
    return new RecentResponse(
        recent.recent(
            owner(caller),
            SubmissionReadParameters.id(problemId, "JDG-101"),
            SubmissionReadParameters.limit(limit)));
  }

  @GetMapping("/me/submissions")
  Page<History> history(
      JwtAuthenticationToken caller,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String verdict,
      @RequestParam(required = false) String language,
      @RequestParam(required = false) String page) {
    return history.history(owner(caller), new Filters(q, verdict, language, page, null));
  }

  private static UUID owner(JwtAuthenticationToken caller) {
    return UUID.fromString(caller.getToken().getSubject());
  }

  record RecentResponse(List<Summary> items) {}
}
