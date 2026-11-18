package com.algoprep.identity.application.ports.in.classes;

import com.algoprep.identity.application.dto.classes.JoinClassByCodeCommand;
import com.algoprep.identity.domain.model.classes.ClassRef;

/** Joins a class with an invite code (#25). */
public interface JoinClassByCodeUseCase {
  ClassRef join(JoinClassByCodeCommand command);
}
