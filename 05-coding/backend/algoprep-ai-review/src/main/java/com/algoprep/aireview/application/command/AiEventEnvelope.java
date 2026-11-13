package com.algoprep.aireview.application.command;

import com.algoprep.aireview.domain.model.AiOutboxEvent;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** The envelope of DD api F5 section 33: event id, type, time, schema version and the payload. */
final class AiEventEnvelope {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private AiEventEnvelope() {}

  /** Throws a RuntimeException when the stored payload is not valid JSON. */
  static String of(AiOutboxEvent event) {
    ObjectNode root = JSON.createObjectNode();
    root.put("eventId", event.id().toString());
    root.put("eventType", event.eventType());
    root.put("occurredAt", event.createdAt().toString());
    root.put("schemaVersion", 1);
    root.set("payload", JSON.readTree(event.payload()));
    return JSON.writeValueAsString(root);
  }
}
