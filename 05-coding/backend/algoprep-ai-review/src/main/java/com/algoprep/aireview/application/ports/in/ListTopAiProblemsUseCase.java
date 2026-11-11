package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.application.dto.TopProblemItem;
import java.util.List;

/** Endpoint 31 (last 30 days). */
public interface ListTopAiProblemsUseCase {
  List<TopProblemItem> topProblems(int limit);
}
