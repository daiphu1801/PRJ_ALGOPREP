package com.algoprep.aireview.application.ports.in.interview;

import com.algoprep.aireview.application.dto.interview.MySessionFilter;
import com.algoprep.aireview.application.dto.interview.MySessionPage;

/**
 * Endpoint 31A (DD F5): the caller's own finished sessions; PostgreSQL only, so it works while the
 * LLM is down.
 */
public interface ListMyInterviewSessionsUseCase {
  MySessionPage list(MySessionFilter filter);
}
