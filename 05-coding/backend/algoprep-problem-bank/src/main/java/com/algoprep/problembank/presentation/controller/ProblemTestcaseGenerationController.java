package com.algoprep.problembank.presentation.controller;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.problembank.application.dto.AuthoringScope;
import com.algoprep.problembank.application.dto.TestcaseGeneration;
import com.algoprep.problembank.application.ports.in.GenerateTestcasesUseCase;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint 48, DD F2 section 49. The body is empty on purpose: the whole set is always generated.
 * The caller is the token subject; an id that is not a UUID is the same 404 as a missing problem.
 */
@RestController
@RequestMapping("/api/v1/problem-bank/authoring/problems/{problemId}/testcases")
class ProblemTestcaseGenerationController {
  private final GenerateTestcasesUseCase generation;

  ProblemTestcaseGenerationController(GenerateTestcasesUseCase generation) {
    this.generation = generation;
  }

  @PostMapping("/generate")
  @PreAuthorize("hasAuthority('TESTCASE_MANAGEMENT:CREATE')")
  TestcaseGeneration.Result generate(
      JwtAuthenticationToken caller, @PathVariable String problemId) {
    boolean admin =
        caller.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    return generation.generate(
        new TestcaseGeneration.Command(
            new AuthoringScope(UUID.fromString(caller.getToken().getSubject()), admin),
            parse(problemId)));
  }

  private static UUID parse(String raw) {
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException e) {
      throw new ResourceNotFoundException("PRB-010", "Problem not found");
    }
  }
}
