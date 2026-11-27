package com.algoprep.aireview.application.ports.in.usage;

import com.algoprep.aireview.application.dto.usage.TopProblemItem;
import java.util.List;

/** Endpoint 31 (last 30 days). */
public interface ListTopAiProblemsUseCase {
  List<TopProblemItem> topProblems(int limit);
}
