package com.algoprep.judge.application.query;

import com.algoprep.common.exception.ResourceNotFoundException;
import java.util.UUID;

final class SubmissionReadQueries {
  private SubmissionReadQueries() {}

  static ResourceNotFoundException missing(String code) {
    return new ResourceNotFoundException(code, "Submission resource not found");
  }

  static UUID id(String value, String code) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException failure) {
      throw missing(code);
    }
  }

  static int page(String value, long count) {
    int requested;
    try {
      requested = Integer.parseInt(value);
    } catch (NumberFormatException failure) {
      requested = 1;
    }
    return (int) Math.clamp(requested, 1, Math.max(1, (count + 19) / 20));
  }

  static String keyword(String value) {
    String q = value == null ? "" : value.strip();
    return q.substring(0, Math.min(100, q.length()));
  }
}
