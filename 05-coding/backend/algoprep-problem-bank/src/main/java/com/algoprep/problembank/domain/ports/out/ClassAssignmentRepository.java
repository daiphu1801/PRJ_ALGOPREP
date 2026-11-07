package com.algoprep.problembank.domain.ports.out;

import com.algoprep.problembank.domain.model.ClassAssignmentModels.ActiveRow;
import com.algoprep.problembank.domain.model.ClassAssignmentModels.AssignablePage;
import com.algoprep.problembank.domain.model.ClassAssignmentModels.ProblemState;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Assignment reads and writes; writes join the caller's transaction. */
public interface ClassAssignmentRepository {
  /**
   * Active assignments of the classes with the problem's display data; a withdrawn problem stays,
   * flagged. {@code q} matches the title.
   */
  List<ActiveRow> active(Set<UUID> classIds, String q);

  /** Published problems not actively assigned to the class; one removed earlier is listed again. */
  AssignablePage assignable(UUID classId, String q, int page, int pageSize);

  Optional<ProblemState> problemState(UUID problemId);

  /** True when a new active row was created, false when one already existed. */
  boolean insertActive(UUID problemId, UUID classId, UUID actorId);

  /** Soft removal; the removal time, or empty when no active row exists. */
  Optional<Instant> deactivate(UUID classId, UUID problemId);
}
