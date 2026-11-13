package com.algoprep.harness.presentation.controller;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.common.functionspec.Language;
import com.algoprep.harness.application.dto.GetStarterCodeQuery;
import com.algoprep.harness.application.dto.SubmissionModel;
import com.algoprep.harness.application.ports.in.GetStarterCodeUseCase;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class StarterCodeController {
  private final GetStarterCodeUseCase starter;

  StarterCodeController(@Lazy GetStarterCodeUseCase starter) {
    this.starter = starter;
  }

  @GetMapping("/api/v1/harness/problems/{problemId}/starter-code")
  @PreAuthorize("isAuthenticated()")
  StarterCodeDto get(
      JwtAuthenticationToken caller,
      @PathVariable String problemId,
      @RequestParam(required = false) String language,
      @RequestParam SubmissionModel model) {
    var result =
        starter.getStarterCode(
            new GetStarterCodeQuery(
                id(problemId),
                language(language),
                model,
                UUID.fromString(caller.getToken().getSubject()),
                category(caller)));
    return new StarterCodeDto(result.language(), result.submissionModel(), result.sourceCode());
  }

  private static UUID id(String value) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException invalid) {
      throw new ResourceNotFoundException("HRN-001", "Problem specification not found");
    }
  }

  private static Language language(String value) {
    try {
      return Language.valueOf(value == null ? "" : value);
    } catch (IllegalArgumentException invalid) {
      throw new DomainRuleViolationException("HRN-010", "Unsupported starter language");
    }
  }

  private static String category(JwtAuthenticationToken caller) {
    return caller.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .filter(Set.of("ROLE_STUDENT", "ROLE_INSTRUCTOR", "ROLE_ADMIN")::contains)
        .map(value -> value.substring("ROLE_".length()))
        .findFirst()
        .orElseThrow(() -> new AccessDeniedException("Starter access denied"));
  }

  record StarterCodeDto(Language language, SubmissionModel submissionModel, String sourceCode) {}
}
