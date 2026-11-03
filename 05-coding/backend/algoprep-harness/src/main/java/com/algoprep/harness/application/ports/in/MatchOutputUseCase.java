package com.algoprep.harness.application.ports.in;

import com.algoprep.harness.application.dto.MatchOutputCommand;
import com.algoprep.harness.application.dto.MatchResult;

/** P4, DD F3 api section 6.2: deterministic output matching without I/O or shared mutable state. */
public interface MatchOutputUseCase {
  MatchResult match(MatchOutputCommand command);
}
