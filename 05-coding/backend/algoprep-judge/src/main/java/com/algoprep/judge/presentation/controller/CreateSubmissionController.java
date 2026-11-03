package com.algoprep.judge.presentation.controller;

import com.algoprep.judge.application.dto.QueuedSubmissionResult;
import com.algoprep.judge.application.dto.RecordPendingSubmissionCommand;
import com.algoprep.judge.application.ports.in.CreateSubmissionUseCase;
import com.algoprep.judge.domain.exception.SubmissionAdmissionException;
import com.algoprep.judge.domain.model.PendingSubmission.Owner;
import com.algoprep.judge.domain.model.PendingSubmission.Program;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
class CreateSubmissionController {
  private final CreateSubmissionUseCase submissions;

  CreateSubmissionController(CreateSubmissionUseCase submissions) {
    this.submissions = submissions;
  }

  @PostMapping("/api/v1/judge/problems/{problemId}/submissions")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('STUDENT')")
  Ticket create(
      JwtAuthenticationToken caller, @PathVariable String problemId, @RequestBody JsonNode body) {
    var command =
        new RecordPendingSubmissionCommand(
            new Owner(UUID.fromString(caller.getToken().getSubject()), problemId(problemId)),
            new Program(
                text(body, "language"),
                text(body, "submissionModel"),
                text(body, "sourceCode"),
                text(body, "sourceInputMethod")),
            text(body, "fileName"));
    return ticket(submissions.create(command));
  }

  private static UUID problemId(String value) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      throw new SubmissionAdmissionException("JDG-101");
    }
  }

  private static String text(JsonNode body, String field) {
    JsonNode value = body.get(field);
    return value != null && value.isString() ? value.asString() : null;
  }

  private static Ticket ticket(QueuedSubmissionResult result) {
    var ticket = result.ticket();
    return new Ticket(
        ticket.submissionId(),
        result.currentStatus(),
        ticket.programType().language(),
        ticket.programType().submissionModel(),
        ticket.submittedAt(),
        ticket.totalCountHint());
  }

  record Ticket(
      UUID submissionId,
      String status,
      String language,
      String submissionModel,
      Instant submittedAt,
      Integer totalCountHint) {}
}
