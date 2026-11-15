package com.algoprep.identity.application.ports.in.classes;

import com.algoprep.identity.application.dto.classes.CreateClassCommand;
import com.algoprep.identity.application.dto.classes.CreatedClass;

/** Creates a class together with its first invite code (#20). */
public interface CreateClassUseCase {
  CreatedClass create(CreateClassCommand command);
}
