package com.algoprep.problembank.presentation.controller;

import com.algoprep.problembank.application.dto.SuggestedProblemViews.SuggestedProblems;
import com.algoprep.problembank.application.ports.in.SuggestedProblemsUseCase;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** #12 (DD F2 section 13). The ranking is about the token subject; no route takes a user id. */
@RestController
@RequestMapping("/api/v1/problem-bank")
@PreAuthorize("isAuthenticated()")
class SuggestedProblemController {
  private final SuggestedProblemsUseCase suggestions;

  SuggestedProblemController(SuggestedProblemsUseCase suggestions) {
    this.suggestions = suggestions;
  }

  @GetMapping("/suggested-problems")
  SuggestedProblems suggested(
      JwtAuthenticationToken caller, @RequestParam Map<String, String> params) {
    return suggestions.suggest(UUID.fromString(caller.getToken().getSubject()), params);
  }
}
