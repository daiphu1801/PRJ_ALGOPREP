package com.algoprep.judge.presentation.controller;

import com.algoprep.judge.application.dto.ExecutionLimitsView;
import com.algoprep.judge.application.dto.SampleRun.Command;
import com.algoprep.judge.application.dto.SampleRun.Result;
import com.algoprep.judge.application.ports.in.GetEffectiveExecutionLimitsUseCase;
import com.algoprep.judge.application.ports.in.RunSampleUseCase;
import com.algoprep.judge.domain.exception.SubmissionAdmissionException;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@PreAuthorize("hasRole('STUDENT')")
class RunSampleController {
  private final RunSampleUseCase runs;
  private final GetEffectiveExecutionLimitsUseCase limits;

  RunSampleController(RunSampleUseCase runs, GetEffectiveExecutionLimitsUseCase limits) {
    this.runs = runs;
    this.limits = limits;
  }

  @GetMapping("/api/v1/judge/problems/{problemId}/execution-limits")
  ExecutionLimitsView limits(
      @PathVariable String problemId, @RequestParam(required = false) String language) {
    return limits.get(id(problemId), language);
  }

  @PostMapping("/api/v1/judge/problems/{problemId}/run-sample")
  Result run(
      JwtAuthenticationToken caller, @PathVariable String problemId, @RequestBody JsonNode body) {
    return runs.run(
        new Command(
            UUID.fromString(caller.getToken().getSubject()),
            id(problemId),
            text(body, "language"),
            text(body, "submissionModel"),
            text(body, "sourceCode")));
  }

  private static UUID id(String value) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException invalid) {
      throw new SubmissionAdmissionException("JDG-101");
    }
  }

  private static String text(JsonNode body, String field) {
    var value = body.get(field);
    return value != null && value.isString() ? value.asString() : null;
  }
}
