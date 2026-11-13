package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.domain.model.ClassProblemAssignment;

/** Adds one "problem assigned to the class" entry to the activity feed, once per event id. */
public interface ApplyProblemAssignedToClassUseCase {

  /** True when an entry was added, false when the event was a redelivery or its class is gone. */
  boolean apply(ClassProblemAssignment assignment);
}
