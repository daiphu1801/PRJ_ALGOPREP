package com.algoprep.problembank.application.ports.in;

import com.algoprep.problembank.application.dto.AuthoringScope;
import com.algoprep.problembank.application.dto.BulkProblemActionResult;
import java.util.List;
import java.util.UUID;

/**
 * The five bulk actions #28 to #32 (DD F2 sections 29 to 33). One interface rather than five
 * because they share validation and the per-problem failure report; every method handles each id in
 * its own transaction.
 */
public interface BulkProblemActionsUseCase {

  /** 1 to 20 ids: each publish runs the reference solution in the sandbox. */
  BulkProblemActionResult publish(AuthoringScope scope, List<UUID> problemIds);

  BulkProblemActionResult unpublish(AuthoringScope scope, List<UUID> problemIds);

  /** An unknown {@code levelId} refuses the whole batch with 422 PRB-039. */
  BulkProblemActionResult changeDifficulty(
      AuthoringScope scope, List<UUID> problemIds, UUID levelId);

  /** An unknown {@code topicId} refuses the whole batch with 422 PRB-039. */
  BulkProblemActionResult assignTopic(AuthoringScope scope, List<UUID> problemIds, UUID topicId);

  BulkProblemActionResult delete(AuthoringScope scope, List<UUID> problemIds);
}
