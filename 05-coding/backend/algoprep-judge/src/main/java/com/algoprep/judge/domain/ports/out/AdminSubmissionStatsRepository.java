package com.algoprep.judge.domain.ports.out;

import com.algoprep.judge.domain.model.metrics.AdminSubmissionStats.DayCount;
import com.algoprep.judge.domain.model.metrics.AdminSubmissionStats.LanguageDay;
import com.algoprep.judge.domain.model.metrics.AdminSubmissionStats.MonthCount;
import com.algoprep.judge.domain.model.metrics.AdminSubmissionStats.ProblemCount;
import com.algoprep.judge.domain.model.metrics.AdminSubmissionStats.Totals;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface AdminSubmissionStatsRepository {
  Instant now();

  Totals totals(Instant asOf);

  List<DayCount> daily(Instant from, Instant asOf);

  List<LanguageDay> languages(Instant from, Instant asOf);

  Map<String, Long> verdicts(Instant asOf);

  List<MonthCount> months(Instant from, Instant asOf);

  List<ProblemCount> problems(Set<UUID> eligibleIds, Instant asOf);
}
