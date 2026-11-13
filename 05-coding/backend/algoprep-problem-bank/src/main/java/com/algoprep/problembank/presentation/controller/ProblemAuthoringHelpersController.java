package com.algoprep.problembank.presentation.controller;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.problembank.application.dto.DuplicateSourceView;
import com.algoprep.problembank.application.dto.ProblemAuthoringQuery;
import com.algoprep.problembank.application.dto.ProblemStatsView;
import com.algoprep.problembank.application.dto.SaveAiContextCommand;
import com.algoprep.problembank.application.ports.in.GetDuplicateSourceUseCase;
import com.algoprep.problembank.application.ports.in.GetProblemStatsUseCase;
import com.algoprep.problembank.application.ports.in.SaveAiAuthoringContextUseCase;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authoring #34 (duplicate source) and #38 (AI context), DD F2 sections 35 and 39. The caller is
 * always the token subject; an id that is not a UUID is the same 404 PRB-010 as a missing problem.
 */
@RestController
@RequestMapping("/api/v1/problem-bank/authoring/problems")
class ProblemAuthoringHelpersController {
  private final GetDuplicateSourceUseCase duplicateSource;
  private final SaveAiAuthoringContextUseCase aiContext;
  private final GetProblemStatsUseCase stats;

  ProblemAuthoringHelpersController(
      GetDuplicateSourceUseCase duplicateSource,
      SaveAiAuthoringContextUseCase aiContext,
      GetProblemStatsUseCase stats) {
    this.duplicateSource = duplicateSource;
    this.aiContext = aiContext;
    this.stats = stats;
  }

  /** Body of #38; the flags are boxed so a missing one is a 400, not a silent false. */
  record AiContextRequest(
      String briefText, Boolean noFullCode, Boolean questionOnly, Instant expectedUpdatedAt) {}

  @GetMapping("/{problemId}/duplicate-source")
  @PreAuthorize("hasAuthority('PROBLEM_AUTHORING:CREATE')")
  DuplicateSourceView duplicateSource(
      JwtAuthenticationToken caller, @PathVariable String problemId) {
    return duplicateSource.get(
        new ProblemAuthoringQuery(id(problemId), actor(caller), admin(caller), false));
  }

  @PutMapping("/{problemId}/ai-context")
  @PreAuthorize("hasAuthority('PROBLEM_AUTHORING:UPDATE')")
  SaveAiContextCommand.Result saveAiContext(
      JwtAuthenticationToken caller,
      @PathVariable String problemId,
      @RequestBody AiContextRequest body) {
    return aiContext.save(
        new SaveAiContextCommand(
            id(problemId),
            actor(caller),
            admin(caller),
            body.briefText(),
            body.noFullCode(),
            body.questionOnly(),
            body.expectedUpdatedAt()));
  }

  @GetMapping("/{problemId}/stats")
  @PreAuthorize("hasAuthority('PROBLEM_AUTHORING:READ')")
  ProblemStatsView stats(JwtAuthenticationToken caller, @PathVariable String problemId) {
    return stats.get(new ProblemAuthoringQuery(id(problemId), actor(caller), admin(caller), false));
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
