package com.algoprep.problembank.application.ports.in.problem;

import com.algoprep.problembank.application.dto.problem.AuthoringDefaults;

public interface ReadAuthoringDefaultsUseCase {
  AuthoringDefaults get();
}
