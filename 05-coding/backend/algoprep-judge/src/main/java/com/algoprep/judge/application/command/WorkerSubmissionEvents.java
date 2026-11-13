package com.algoprep.judge.application.command;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.judge.domain.model.WorkerRecords.Finished;
import com.algoprep.judge.domain.ports.out.SubmissionWorkerRepository;
import java.util.UUID;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

final class WorkerSubmissionEvents {
  private final SubmissionWorkerRepository repository;
  private final IdGeneratorPort ids;
  private final JsonMapper json = JsonMapper.builder().build();

  WorkerSubmissionEvents(SubmissionWorkerRepository repository, IdGeneratorPort ids) {
    this.repository = repository;
    this.ids = ids;
  }

  void append(Finished finished) {
    var state = finished.state();
    var payload = identity(finished);
    payload.put("verdict", state.status());
    boolean error = state.status().equals("SYSTEM_ERROR") || state.status().equals("COMPILE_ERROR");
    payload.set(
        "passedCount", error ? json.nullNode() : json.valueToTree(finished.outcome().passed()));
    payload.set(
        "totalCount", error ? json.nullNode() : json.valueToTree(finished.outcome().total()));
    payload.set(
        "passedTestcaseRatio",
        error ? json.nullNode() : json.valueToTree(finished.outcome().score()));
    payload.put("submittedAt", state.submittedAt().toString());
    payload.put("firstAttemptForProblem", repository.firstAttempt(state));
    appendEvent(finished, "SubmissionGraded", payload);
    if (state.status().equals("ACCEPTED")) {
      appendEvent(finished, "SubmissionAccepted", identity(finished));
    }
  }

  private ObjectNode identity(Finished finished) {
    var state = finished.state();
    return json.createObjectNode()
        .put("submissionId", state.id().toString())
        .put("userId", state.owner().userId().toString())
        .put("problemId", state.owner().problemId().toString())
        .put("language", state.language());
  }

  private void appendEvent(Finished finished, String type, ObjectNode payload) {
    UUID eventId = ids.newId();
    var envelope =
        json.createObjectNode()
            .put("eventId", eventId.toString())
            .put("eventType", type)
            .put("occurredAt", finished.state().updatedAt().toString())
            .put("schemaVersion", 1);
    envelope.set("payload", payload);
    repository.append(eventId, finished.state(), type, json.writeValueAsString(envelope));
  }
}
