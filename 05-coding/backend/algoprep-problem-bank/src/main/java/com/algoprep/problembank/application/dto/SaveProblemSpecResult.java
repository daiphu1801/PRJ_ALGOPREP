package com.algoprep.problembank.application.dto;

import java.time.Instant;
import java.util.List;
import tools.jackson.databind.JsonNode;

/** DD F2 section 38 response, including the timestamp for the next optimistic write. */
public record SaveProblemSpecResult(
    JsonNode spec,
    boolean functionWrapperSupported,
    List<Warning> warnings,
    Instant problemUpdatedAt) {
  public record Warning(String code, String reasonCode, String path, String message) {}
}
