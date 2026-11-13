package com.algoprep.judge.application.dto;

import com.algoprep.common.exception.InvalidRequestException;

public record LearnerListLimit(int value) {
  public LearnerListLimit {
    if (value < 1 || value > 20) {
      throw new InvalidRequestException("JDG-V14", "Invalid submission limit");
    }
  }
}
