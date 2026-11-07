package com.algoprep.problembank.domain.ports.out;

import com.algoprep.problembank.domain.model.ApprovalCounts;
import com.algoprep.problembank.domain.model.ProblemTouch;
import com.algoprep.problembank.domain.model.StoredTestcase;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Testcase writes of DD F2 sections 42 to 45; every call joins the caller's transaction. */
public interface TestcaseWriteRepository {
  ApprovalCounts counts(UUID problemId);

  /** One past the highest position of the problem, 1 when it has none. */
  int nextPosition(UUID problemId);

  /** Locks the row; empty when it does not exist or belongs to another problem. */
  Optional<StoredTestcase> lock(UUID problemId, UUID testcaseId);

  void insert(UUID problemId, StoredTestcase testcase);

  void update(StoredTestcase testcase);

  void delete(UUID testcaseId);

  /** Ids of the problem in grading order, rows locked. */
  List<UUID> lockOrder(UUID problemId);

  /** Writes position 1..n in the order given. */
  void setPositions(List<UUID> orderedIds);

  /** Stamps {@code updated_by} and {@code updated_at} on a problem the caller has locked. */
  ProblemTouch touch(UUID problemId, UUID actorId);

  /** Reads the problem row without changing it. */
  ProblemTouch state(UUID problemId);
}
