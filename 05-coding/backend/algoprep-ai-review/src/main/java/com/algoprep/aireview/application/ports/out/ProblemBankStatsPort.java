package com.algoprep.aireview.application.ports.out;

import com.algoprep.aireview.domain.model.ProblemSummary;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Outbound port served by problem-bank (DD F5 section 1.3, 2 second deadline in the bridge). */
public interface ProblemBankStatsPort {
  /** Unknown ids are absent from the answer. Any failure is a RuntimeException. */
  List<ProblemSummary> problemSummaries(Set<UUID> problemIds);
}
