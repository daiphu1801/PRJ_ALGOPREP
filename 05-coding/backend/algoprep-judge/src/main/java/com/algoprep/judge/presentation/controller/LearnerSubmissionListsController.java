package com.algoprep.judge.presentation.controller;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.judge.application.dto.LearnerListLimit;
import com.algoprep.judge.application.dto.LearnerSubmissionLists.AcceptedItem;
import com.algoprep.judge.application.dto.LearnerSubmissionLists.InProgressItem;
import com.algoprep.judge.application.ports.in.ListMyAcceptedSubmissionsUseCase;
import com.algoprep.judge.application.ports.in.ListMyInProgressProblemsUseCase;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/judge/me")
@PreAuthorize("hasRole('STUDENT')")
class LearnerSubmissionListsController {
  private final ListMyInProgressProblemsUseCase progress;
  private final ListMyAcceptedSubmissionsUseCase accepted;

  LearnerSubmissionListsController(
      ListMyInProgressProblemsUseCase progress, ListMyAcceptedSubmissionsUseCase accepted) {
    this.progress = progress;
    this.accepted = accepted;
  }

  @GetMapping("/in-progress-problems")
  Items<InProgressItem> inProgress(@RequestParam(required = false) String limit) {
    return new Items<>(progress.inProgress(limit(limit, 3)));
  }

  @GetMapping("/accepted-submissions")
  Items<AcceptedItem> accepted(@RequestParam(required = false) String limit) {
    return new Items<>(accepted.accepted(limit(limit, 10)));
  }

  private static LearnerListLimit limit(String value, int defaultValue) {
    try {
      return new LearnerListLimit(value == null ? defaultValue : Integer.parseInt(value));
    } catch (NumberFormatException failure) {
      throw new InvalidRequestException("JDG-V14", "Invalid submission limit");
    }
  }

  record Items<T>(List<T> items) {
    Items {
      items = List.copyOf(items);
    }
  }
}
