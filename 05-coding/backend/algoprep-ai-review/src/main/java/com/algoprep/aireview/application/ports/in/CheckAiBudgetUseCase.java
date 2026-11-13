package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.domain.model.CallerCategory;
import java.util.UUID;

/** P1 (DD F5 section 32.1): the budget gate used by F6 and by F5 itself. */
public interface CheckAiBudgetUseCase {
  /**
   * Returns normally while there is budget; ADMIN always passes. Throws {@code
   * AiBudgetExceededException} (AIR-401) when STUDENT or INSTRUCTOR is locked, and sets {@code
   * locked_at} first when the used total has just reached the limit.
   */
  void check(UUID userId, CallerCategory baseCategory);
}
