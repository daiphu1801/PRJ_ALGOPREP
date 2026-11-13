package com.algoprep.identity.application.ports.out;

import com.algoprep.identity.domain.model.ClassProblemWindow;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Which problems each of the given classes had assigned, and when. Served by problem-bank. A
 * failure throws; an empty list means no assignments, never a fallback, so the caller can tell "not
 * in any class" from "could not ask".
 */
public interface ClassAssignmentLookupPort {
  List<ClassProblemWindow> assignmentsOf(Collection<UUID> classIds);
}
