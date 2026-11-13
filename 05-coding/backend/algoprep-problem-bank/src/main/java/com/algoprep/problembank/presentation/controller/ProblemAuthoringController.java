package com.algoprep.problembank.presentation.controller;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.problembank.application.dto.CreateProblemCommand;
import com.algoprep.problembank.application.dto.CreateProblemCommand.PartialLimits;
import com.algoprep.problembank.application.dto.ProblemAuthoringQuery;
import com.algoprep.problembank.application.dto.ProblemAuthoringView;
import com.algoprep.problembank.application.dto.SaveProblemContentCommand;
import com.algoprep.problembank.application.dto.UpdateProblemStatusCommand;
import com.algoprep.problembank.application.dto.UpdateProblemStatusResult;
import com.algoprep.problembank.application.ports.in.CreateProblemUseCase;
import com.algoprep.problembank.application.ports.in.GetProblemForAuthoringUseCase;
import com.algoprep.problembank.application.ports.in.SaveProblemContentUseCase;
import com.algoprep.problembank.application.ports.in.UpdateProblemPublishStatusUseCase;
import com.algoprep.problembank.domain.exception.ProblemContentInvalidException;
import com.algoprep.problembank.domain.model.ProblemContentData;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authoring #33 (GET), #35 (create) and #36 (replace content), DD F2 sections 34, 36 and 37. The
 * caller is always the token subject. An id that is not a UUID is the same 404 PRB-010 as a missing
 * problem.
 */
@RestController
@RequestMapping("/api/v1/problem-bank/authoring/problems")
class ProblemAuthoringController {
  private final GetProblemForAuthoringUseCase read;
  private final CreateProblemUseCase create;
  private final SaveProblemContentUseCase save;
  private final UpdateProblemPublishStatusUseCase status;

  ProblemAuthoringController(
      GetProblemForAuthoringUseCase read,
      CreateProblemUseCase create,
      SaveProblemContentUseCase save,
      UpdateProblemPublishStatusUseCase status) {
    this.read = read;
    this.create = create;
    this.save = save;
    this.status = status;
  }

  /** Limits as typed; on create a missing one takes the default, on update all three are needed. */
  record LimitsRequest(
      Integer timeLimitMs,
      Integer memoryLimitMb,
      Integer maxOutputSizeKb,
      Integer maxSubmissionsPerHour) {}

  record SolutionRequest(String language, String sourceCode) {}

  record ExampleRequest(UUID id, String input, String output, String explanationMd) {}

  /** Body of #35 and #36; {@code duplicatedFromProblemId} only exists on create. */
  record ProblemRequest(
      String title,
      String statementMd,
      String constraintsMd,
      UUID levelId,
      LimitsRequest limits,
      List<UUID> topicIds,
      List<String> tags,
      SolutionRequest referenceSolution,
      List<ExampleRequest> workedExamples,
      UUID duplicatedFromProblemId,
      Instant expectedUpdatedAt) {}

  /** Body of #39: the target status and the {@code updatedAt} the client last saw. */
  record StatusRequest(String status, Instant expectedUpdatedAt) {}

  @GetMapping("/{problemId}")
  @PreAuthorize("hasAnyAuthority('PROBLEM_AUTHORING:READ', 'TESTCASE_MANAGEMENT:READ')")
  ProblemAuthoringView get(JwtAuthenticationToken caller, @PathVariable String problemId) {
    return read.get(query(caller, id(problemId)));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('PROBLEM_AUTHORING:CREATE')")
  ProblemAuthoringView create(JwtAuthenticationToken caller, @RequestBody ProblemRequest body) {
    var limits = body.limits() == null ? new LimitsRequest(null, null, null, null) : body.limits();
    var created =
        create.create(
            new CreateProblemCommand(
                actor(caller),
                admin(caller),
                content(body, new ProblemContentData.Limits(0, 0, 0, null)),
                new PartialLimits(
                    limits.timeLimitMs(),
                    limits.memoryLimitMb(),
                    limits.maxOutputSizeKb(),
                    limits.maxSubmissionsPerHour()),
                body.duplicatedFromProblemId()));
    return read.get(query(caller, created.id()));
  }

  @PutMapping("/{problemId}/content")
  @PreAuthorize("hasAuthority('PROBLEM_AUTHORING:UPDATE')")
  ProblemAuthoringView replace(
      JwtAuthenticationToken caller,
      @PathVariable String problemId,
      @RequestBody ProblemRequest body) {
    var id = id(problemId);
    var l = body.limits();
    if (l == null || l.timeLimitMs() == null) {
      throw new ProblemContentInvalidException("limits.timeLimitMs", "required");
    }
    if (l.memoryLimitMb() == null) {
      throw new ProblemContentInvalidException("limits.memoryLimitMb", "required");
    }
    if (l.maxOutputSizeKb() == null) {
      throw new ProblemContentInvalidException("limits.maxOutputSizeKb", "required");
    }
    var limits =
        new ProblemContentData.Limits(
            l.timeLimitMs(), l.memoryLimitMb(), l.maxOutputSizeKb(), l.maxSubmissionsPerHour());
    save.save(
        new SaveProblemContentCommand(
            id,
            actor(caller),
            admin(caller),
            true,
            body.expectedUpdatedAt(),
            content(body, limits)));
    return read.get(query(caller, id));
  }

  @PutMapping("/{problemId}/status")
  @PreAuthorize("hasAuthority('PROBLEM_AUTHORING:UPDATE')")
  UpdateProblemStatusResult changeStatus(
      JwtAuthenticationToken caller,
      @PathVariable String problemId,
      @RequestBody StatusRequest body) {
    return status.update(
        new UpdateProblemStatusCommand(
            id(problemId), actor(caller), admin(caller), body.status(), body.expectedUpdatedAt()));
  }

  private static ProblemContentData content(ProblemRequest b, ProblemContentData.Limits limits) {
    var examples = new java.util.ArrayList<ProblemContentData.Example>();
    var source = b.workedExamples() == null ? List.<ExampleRequest>of() : b.workedExamples();
    for (int i = 0; i < source.size(); i++) {
      var e = source.get(i);
      examples.add(
          new ProblemContentData.Example(e.id(), i + 1, e.input(), e.output(), e.explanationMd()));
    }
    var solution =
        b.referenceSolution() == null
            ? null
            : new ProblemContentData.Solution(
                b.referenceSolution().language(), b.referenceSolution().sourceCode());
    return new ProblemContentData(
        b.title(),
        b.statementMd(),
        b.constraintsMd(),
        b.levelId(),
        limits,
        b.topicIds(),
        b.tags(),
        examples,
        solution);
  }

  private static ProblemAuthoringQuery query(JwtAuthenticationToken caller, UUID problemId) {
    boolean testcases =
        caller.getAuthorities().stream()
            .anyMatch(a -> "TESTCASE_MANAGEMENT:READ".equals(a.getAuthority()));
    return new ProblemAuthoringQuery(problemId, actor(caller), admin(caller), testcases);
  }

  private static UUID actor(JwtAuthenticationToken caller) {
    return UUID.fromString(caller.getToken().getSubject());
  }

  private static boolean admin(JwtAuthenticationToken caller) {
    return caller.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
  }

  private static UUID id(String raw) {
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException e) {
      throw new ResourceNotFoundException("PRB-010", "Problem not found");
    }
  }
}
