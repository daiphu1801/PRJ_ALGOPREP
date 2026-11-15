package com.algoprep.problembank.application.ports.in;

import com.algoprep.problembank.application.dto.AuthoringDefaults;

public interface ReadAuthoringDefaultsUseCase {
  AuthoringDefaults get();
}
