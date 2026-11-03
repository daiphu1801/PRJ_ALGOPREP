package com.algoprep.judge.presentation.controller;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.exception.ResourceNotFoundException;
import java.util.UUID;

final class SubmissionReadParameters {
  private SubmissionReadParameters() {}

  static UUID id(String value, String code) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException failure) {
      throw new ResourceNotFoundException(code, "Submission resource not found");
    }
  }

  static int limit(String value) {
    try {
      int limit = value == null ? 10 : Integer.parseInt(value);
      if (limit >= 1 && limit <= 10) {
        return limit;
      }
    } catch (NumberFormatException failure) {
      /* Same validation code for all malformed limits. */
    }
    throw new InvalidRequestException("JDG-V14", "Invalid submission limit");
  }
}
