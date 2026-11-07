package com.algoprep.identity.application.query;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.identity.application.ports.in.ListMyProblemSolveStatesUseCase;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.SolveState;
import com.algoprep.identity.domain.ports.out.MyProgressReadRepository;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Answers only for the ids asked, and only for the caller. Caching belongs to the caller module (DD
 * api F1 row 48).
 */
@Service
class ListMyProblemSolveStatesQueryHandler implements ListMyProblemSolveStatesUseCase {
  static final int MAX_IDS = 100;

  private final MyProgressReadRepository progress;

  ListMyProblemSolveStatesQueryHandler(MyProgressReadRepository progress) {
    this.progress = progress;
  }

  @Override
  @Transactional(readOnly = true)
  public Map<UUID, SolveState> list(UUID userId, String problemIds) {
    return progress.solveStates(userId, parse(problemIds));
  }

  private static Set<UUID> parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw invalid("problemIds is required");
    }
    Set<UUID> ids = new LinkedHashSet<>();
    for (String part : raw.split(",", -1)) {
      try {
        ids.add(UUID.fromString(part.trim()));
      } catch (IllegalArgumentException e) {
        throw invalid("problemIds holds a malformed id");
      }
      if (ids.size() > MAX_IDS) {
        throw invalid("problemIds holds more than " + MAX_IDS + " ids");
      }
    }
    return ids;
  }

  private static DomainRuleViolationException invalid(String message) {
    return new DomainRuleViolationException(IdentityErrorCodes.PROBLEM_IDS_INVALID, message);
  }
}
