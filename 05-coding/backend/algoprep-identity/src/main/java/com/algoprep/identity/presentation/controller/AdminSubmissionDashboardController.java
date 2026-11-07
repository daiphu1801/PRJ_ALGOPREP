package com.algoprep.identity.presentation.controller;

import com.algoprep.identity.application.ports.in.GetAdminSubmissionBlocksUseCase;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.Days;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.Difficulty;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.Languages;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.Months;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.TopProblems;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.Verdicts;
import com.algoprep.identity.domain.model.StatSummary;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The judge-backed blocks of the admin overview, #43 and #67 to #72 (DD api F1 section 0.8.5). One
 * endpoint per block so one failing source answers {@code 503} for its own block only.
 */
@RestController
@RequestMapping("/api/v1/identity/admin/dashboard")
class AdminSubmissionDashboardController {
  private final GetAdminSubmissionBlocksUseCase blocks;

  AdminSubmissionDashboardController(GetAdminSubmissionBlocksUseCase blocks) {
    this.blocks = blocks;
  }

  @GetMapping("/cross-module")
  @PreAuthorize("hasRole('ADMIN')")
  StatSummary submissionsSummary() {
    return blocks.submissionsSummary();
  }

  @GetMapping("/submissions/by-language")
  @PreAuthorize("hasRole('ADMIN')")
  Languages byLanguage() {
    return blocks.byLanguage();
  }

  @GetMapping("/submissions/verdicts")
  @PreAuthorize("hasRole('ADMIN')")
  Verdicts verdicts() {
    return blocks.verdicts();
  }

  @GetMapping("/submissions/difficulty")
  @PreAuthorize("hasRole('ADMIN')")
  Difficulty byDifficulty() {
    return blocks.byDifficulty();
  }

  @GetMapping("/submissions/by-day")
  @PreAuthorize("hasRole('ADMIN')")
  Days byDay() {
    return blocks.byDay();
  }

  @GetMapping("/submissions/by-month")
  @PreAuthorize("hasRole('ADMIN')")
  Months byMonth() {
    return blocks.byMonth();
  }

  @GetMapping("/top-problems")
  @PreAuthorize("hasRole('ADMIN')")
  TopProblems topProblems() {
    return blocks.topProblems();
  }
}
