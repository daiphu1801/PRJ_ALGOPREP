package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.application.dto.JoinClassByCodeCommand;
import com.algoprep.identity.domain.model.ClassRef;

/** Joins a class with an invite code (#25). */
public interface JoinClassByCodeUseCase {
  ClassRef join(JoinClassByCodeCommand command);
}
