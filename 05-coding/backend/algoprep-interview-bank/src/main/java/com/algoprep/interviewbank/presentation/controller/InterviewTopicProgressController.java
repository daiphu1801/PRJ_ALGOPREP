package com.algoprep.interviewbank.presentation.controller;

import com.algoprep.interviewbank.application.ports.in.ReadMyInterviewTopicProgressUseCase;
import com.algoprep.interviewbank.domain.model.TopicProgress;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** DD api F6 section 8. No parameters: the caller id comes from the token only. */
@RestController
@RequestMapping("/api/v1/interview-bank/me")
class InterviewTopicProgressController {
  private final ReadMyInterviewTopicProgressUseCase progress;

  InterviewTopicProgressController(ReadMyInterviewTopicProgressUseCase progress) {
    this.progress = progress;
  }

  record Item(
      UUID topicId,
      String topicCode,
      String topicName,
      int sortOrder,
      int questionsAttempted,
      int totalQuestions,
      int completionPercent,
      Instant lastPracticedAt) {}

  record Items(List<Item> items) {}

  @GetMapping("/topic-progress")
  Items topicProgress(JwtAuthenticationToken caller) {
    List<TopicProgress> rows = progress.listFor(UUID.fromString(caller.getToken().getSubject()));
    return new Items(
        rows.stream()
            .map(
                r ->
                    new Item(
                        r.topicId(),
                        r.topicCode(),
                        r.topicName(),
                        r.sortOrder(),
                        r.questionsAttempted(),
                        r.totalQuestions(),
                        r.completionPercent(),
                        r.lastPracticedAt()))
            .toList());
  }
}
