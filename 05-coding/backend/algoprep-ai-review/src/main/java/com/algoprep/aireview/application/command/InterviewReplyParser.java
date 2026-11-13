package com.algoprep.aireview.application.command;

import com.algoprep.aireview.domain.exception.AiInvalidResponseException;
import com.algoprep.aireview.domain.llm.JsonExtractor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Parses the turn reply {@code {"message": "...", "stageComplete": bool}} (BR-AI-07 step 9). A
 * missing or blank message, or a {@code stageComplete} that is not a boolean, is AIR-502.
 */
final class InterviewReplyParser {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  record Reply(String message, boolean stageComplete) {}

  private InterviewReplyParser() {}

  static Reply parse(String raw) {
    String json =
        JsonExtractor.firstJsonObject(raw)
            .orElseThrow(() -> new AiInvalidResponseException("Interview reply is not JSON"));
    JsonNode node;
    try {
      node = JSON.readTree(json);
    } catch (RuntimeException broken) {
      throw new AiInvalidResponseException("Interview reply is not valid JSON", broken);
    }
    JsonNode message = node.get("message");
    if (message == null || !message.isString() || message.asString().isBlank()) {
      throw new AiInvalidResponseException("Interview reply has no message");
    }
    JsonNode complete = node.get("stageComplete");
    if (complete != null && !complete.isNull() && !complete.isBoolean()) {
      throw new AiInvalidResponseException("stageComplete must be a boolean");
    }
    return new Reply(message.asString().strip(), complete != null && complete.asBoolean(false));
  }

  /** A hint is plain text; a model that wrapped it in the turn JSON anyway is unwrapped. */
  static String plainText(String raw) {
    String text = raw == null ? "" : raw.strip();
    if (text.startsWith("{")) {
      return parse(text).message();
    }
    if (text.isEmpty()) {
      throw new AiInvalidResponseException("Interview hint is empty");
    }
    return text;
  }
}
