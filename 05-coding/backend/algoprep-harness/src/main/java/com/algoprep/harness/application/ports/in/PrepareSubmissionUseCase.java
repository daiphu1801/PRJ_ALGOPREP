package com.algoprep.harness.application.ports.in;

import com.algoprep.harness.application.dto.PrepareSubmissionCommand;
import com.algoprep.harness.application.dto.PreparedProgram;

public interface PrepareSubmissionUseCase {
  PreparedProgram prepare(PrepareSubmissionCommand command);
}
