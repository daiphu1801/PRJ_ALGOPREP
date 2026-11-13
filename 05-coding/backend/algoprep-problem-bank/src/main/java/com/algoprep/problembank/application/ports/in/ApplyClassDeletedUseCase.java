package com.algoprep.problembank.application.ports.in;

import java.util.UUID;

public interface ApplyClassDeletedUseCase {
  /** BR-19: soft-remove assignments, preserving problems and submission history. */
  int apply(UUID classId);
}
