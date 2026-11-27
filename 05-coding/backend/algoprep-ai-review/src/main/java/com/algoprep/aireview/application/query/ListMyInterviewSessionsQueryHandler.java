package com.algoprep.aireview.application.query;

import com.algoprep.aireview.application.dto.interview.MySessionFilter;
import com.algoprep.aireview.application.dto.interview.MySessionPage;
import com.algoprep.aireview.application.ports.in.interview.ListMyInterviewSessionsUseCase;
import com.algoprep.aireview.domain.ports.out.InterviewHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Endpoint 31A: one read, the filter already validated by the controller. */
@Service
@Transactional(readOnly = true)
class ListMyInterviewSessionsQueryHandler implements ListMyInterviewSessionsUseCase {
  private final InterviewHistoryRepository history;

  ListMyInterviewSessionsQueryHandler(InterviewHistoryRepository history) {
    this.history = history;
  }

  @Override
  public MySessionPage list(MySessionFilter filter) {
    return history.search(filter);
  }
}
