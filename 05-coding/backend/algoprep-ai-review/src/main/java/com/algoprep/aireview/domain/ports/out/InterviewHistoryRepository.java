package com.algoprep.aireview.domain.ports.out;

import com.algoprep.aireview.application.dto.interview.MySessionFilter;
import com.algoprep.aireview.application.dto.interview.MySessionPage;

/** Read-only paged search over one learner's COMPLETED and ABANDONED mock-interview sessions. */
public interface InterviewHistoryRepository {
  MySessionPage search(MySessionFilter filter);
}
