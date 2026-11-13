package com.algoprep.interviewbank.application.ports.in;

import com.algoprep.interviewbank.domain.model.TopicProgress;
import java.util.List;
import java.util.UUID;

/**
 * Shared by GET /me/topic-progress and the in-process entry for identity (DD api F6 section 27).
 */
public interface ReadMyInterviewTopicProgressUseCase {
  List<TopicProgress> listFor(UUID userId);
}
