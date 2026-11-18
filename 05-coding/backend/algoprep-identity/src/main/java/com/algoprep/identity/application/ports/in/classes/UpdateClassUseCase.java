package com.algoprep.identity.application.ports.in.classes;

import com.algoprep.identity.application.dto.classes.UpdateClassCommand;
import com.algoprep.identity.domain.model.classes.InstructorClass;

/** Edits name, description and schedule of an owned class (#21). */
public interface UpdateClassUseCase {
  InstructorClass update(UpdateClassCommand command);
}
