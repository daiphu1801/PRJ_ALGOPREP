package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.application.dto.CreateClassCommand;
import com.algoprep.identity.application.dto.CreatedClass;

/** Creates a class together with its first invite code (#20). */
public interface CreateClassUseCase {
  CreatedClass create(CreateClassCommand command);
}
