package com.algoprep.problembank.presentation.controller;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.problembank.application.dto.StudentProblemViews;
import com.algoprep.problembank.application.ports.in.ReadPublishedProblemsUseCase;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Overview;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Workspace;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Learner-facing reads #1, #2, #5, #6, #7 (DD F2 sections 2, 3, 6, 7, 8); any signed-in account may
 * call them. Query parameters are passed on as text because they are read leniently. An id that is
 * not a UUID is the same 404 as a problem that is not published.
 */
@RestController
@RequestMapping("/api/v1/problem-bank/problems")
@PreAuthorize("isAuthenticated()")
class PublishedProblemController {
  private final ReadPublishedProblemsUseCase problems;

  PublishedProblemController(ReadPublishedProblemsUseCase problems) {
    this.problems = problems;
  }

  @GetMapping
  StudentProblemViews.ProblemList list(
      JwtAuthenticationToken caller, @RequestParam Map<String, String> params) {
    return problems.list(user(caller), params);
  }

  @GetMapping("/random")
  StudentProblemViews.RandomProblem random(
      JwtAuthenticationToken caller, @RequestParam Map<String, String> params) {
    return problems.random(user(caller), params);
  }

  @GetMapping("/{problemId}/workspace")
  Workspace workspace(JwtAuthenticationToken caller, @PathVariable String problemId) {
    return problems.workspace(user(caller), problem(problemId));
  }

  @GetMapping("/{problemId}/overview")
  Overview overview(JwtAuthenticationToken caller, @PathVariable String problemId) {
    return problems.overview(user(caller), problem(problemId));
  }

  @GetMapping("/{problemId}/sample-testcases")
  StudentProblemViews.SampleList samples(@PathVariable String problemId) {
    return problems.samples(problem(problemId));
  }

  private static UUID user(JwtAuthenticationToken caller) {
    return UUID.fromString(caller.getToken().getSubject());
  }

  private static UUID problem(String raw) {
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException e) {
      throw new ResourceNotFoundException("PRB-010", "Problem not found");
    }
  }
}
