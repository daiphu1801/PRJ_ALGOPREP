package com.algoprep.judge.presentation.controller;

import com.algoprep.judge.application.dto.QueueSettingsChange;
import com.algoprep.judge.domain.exception.AdminConfigException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

final class AdminQueueRequest {
  private static final JsonMapper JSON =
      JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();

  private AdminQueueRequest() {}

  static JsonNode body(String request, String code) {
    if (request == null) {
      throw new AdminConfigException(400, code);
    }
    try {
      var body = JSON.readTree(request);
      if (body == null || !body.isObject()) {
        throw new AdminConfigException(400, code);
      }
      return body;
    } catch (JacksonException failure) {
      throw new AdminConfigException(400, code);
    }
  }

  static boolean bool(JsonNode body, String field) {
    if (!body.path(field).isBoolean()) {
      throw new AdminConfigException(400, "JDG-V16");
    }
    return body.path(field).booleanValue();
  }

  static QueueSettingsChange settings(String request) {
    var body = body(request, "JDG-V09");
    return new QueueSettingsChange(
        integer(body, "autoscaleJobThreshold"),
        integer(body, "jobsPerPage"),
        integer(body, "errorRetentionHours"),
        integer(body, "latencyCapSeconds"));
  }

  private static int integer(JsonNode body, String name) {
    var field = body.path(name);
    if (!field.isIntegralNumber() || !field.canConvertToInt()) {
      throw new AdminConfigException(400, "JDG-V09");
    }
    return field.intValue();
  }
}
