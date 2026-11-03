package com.algoprep.problembank.presentation.controller;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.problembank.application.dto.SaveProblemSpecCommand;
import com.algoprep.problembank.application.dto.SaveProblemSpecResult;
import com.algoprep.problembank.application.ports.in.SaveProblemSpecUseCase;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/v1/problem-bank/authoring/problems")
class ProblemSpecController {
  private final SaveProblemSpecUseCase specs;

  ProblemSpecController(SaveProblemSpecUseCase specs) {
    this.specs = specs;
  }

  @PutMapping("/{problemId}/spec")
  @PreAuthorize("hasAuthority('PROBLEM_AUTHORING:UPDATE')")
  SaveProblemSpecResult save(
      JwtAuthenticationToken caller, @PathVariable String problemId, @RequestBody String request) {
    try {
      JsonNode body =
          JsonMapper.builder()
              .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
              .build()
              .readTree(request);
      var id = UUID.fromString(problemId);
      if (!body.isObject() || !body.path("expectedUpdatedAt").isString()) {
        throw invalid();
      }
      var expected = Instant.parse(body.path("expectedUpdatedAt").asString());
      boolean admin =
          caller.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
      return specs.save(
          new SaveProblemSpecCommand(
              id, UUID.fromString(caller.getToken().getSubject()), admin, expected, body));
    } catch (IllegalArgumentException | DateTimeParseException | JacksonException failure) {
      throw invalid();
    }
  }

  private InvalidRequestException invalid() {
    return new InvalidRequestException("PRB-001", "Invalid specification request");
  }
}
