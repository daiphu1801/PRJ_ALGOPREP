package com.algoprep.interviewbank.application.query;

import com.algoprep.interviewbank.application.ports.in.ReadMyInterviewTopicProgressUseCase;
import com.algoprep.interviewbank.domain.model.TopicProgress;
import com.algoprep.interviewbank.domain.ports.out.TopicProgressRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReadMyInterviewTopicProgressQueryHandler implements ReadMyInterviewTopicProgressUseCase {
  private final TopicProgressRepository repository;

  ReadMyInterviewTopicProgressQueryHandler(TopicProgressRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<TopicProgress> listFor(UUID userId) {
    return repository.listFor(userId);
  }
}
