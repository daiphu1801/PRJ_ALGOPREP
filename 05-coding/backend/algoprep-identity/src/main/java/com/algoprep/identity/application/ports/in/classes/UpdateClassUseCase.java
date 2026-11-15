package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.application.dto.UpdateClassCommand;
import com.algoprep.identity.domain.model.InstructorClass;

/** Edits name, description and schedule of an owned class (#21). */
public interface UpdateClassUseCase {
  InstructorClass update(UpdateClassCommand command);
}
