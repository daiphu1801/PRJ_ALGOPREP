package com.algoprep.problembank.presentation.controller;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.problembank.application.dto.TestcaseGeneration;
import com.algoprep.problembank.application.ports.in.AiGenerationSettingsUseCase;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** Endpoints 55 and 56, DD F2 sections 56 and 57: the cap of AI generations per problem. */
@RestController
@RequestMapping("/api/v1/problem-bank/settings/ai-testcase-generation")
class AiGenerationSettingsController {
  private final AiGenerationSettingsUseCase settings;

  AiGenerationSettingsController(AiGenerationSettingsUseCase settings) {
    this.settings = settings;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('AI_CONFIG:READ')")
  TestcaseGeneration.Settings get() {
    return settings.get();
  }

  /** Anything but a whole number under {@code maxPerProblem} is 400 PRB-001. */
  @PutMapping
  @PreAuthorize("hasAuthority('AI_CONFIG:UPDATE')")
  TestcaseGeneration.Settings update(JwtAuthenticationToken caller, @RequestBody JsonNode body) {
    JsonNode value = body == null ? null : body.get("maxPerProblem");
    if (value == null || !value.isInt()) {
      throw new InvalidRequestException("PRB-001", "maxPerProblem must be a whole number");
    }
    return settings.update(
        new TestcaseGeneration.UpdateSettings(
            UUID.fromString(caller.getToken().getSubject()), value.asInt()));
  }
}
