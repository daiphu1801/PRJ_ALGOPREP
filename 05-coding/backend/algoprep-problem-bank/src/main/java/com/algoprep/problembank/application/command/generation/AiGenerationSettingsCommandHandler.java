package com.algoprep.problembank.application.command;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.problembank.application.dto.TestcaseGeneration.Settings;
import com.algoprep.problembank.application.dto.TestcaseGeneration.UpdateSettings;
import com.algoprep.problembank.application.ports.in.AiGenerationSettingsUseCase;
import com.algoprep.problembank.domain.model.CatalogAuditEntry;
import com.algoprep.problembank.domain.ports.out.ProblemAuditPort;
import com.algoprep.problembank.domain.ports.out.ProblemAuthoringReadRepository;
import com.algoprep.problembank.domain.ports.out.TestcaseGenerationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DD F2 sections 56 and 57. A change locks the singleton, writes the new cap and the audit row in
 * one transaction, so a failed audit rolls the cap back. Lowering the cap never clears the used
 * counter of any problem.
 */
@Service
class AiGenerationSettingsCommandHandler implements AiGenerationSettingsUseCase {
  static final int MIN = 1;
  static final int MAX = 10;
  static final String ACTION = "AI_TESTCASE_GENERATION_LIMIT_CHANGED";

  private final ProblemAuthoringReadRepository reads;
  private final TestcaseGenerationRepository settings;
  private final ProblemAuditPort audit;

  AiGenerationSettingsCommandHandler(
      ProblemAuthoringReadRepository reads,
      TestcaseGenerationRepository settings,
      ProblemAuditPort audit) {
    this.reads = reads;
    this.settings = settings;
    this.audit = audit;
  }

  @Override
  @Transactional(readOnly = true)
  public Settings get() {
    return new Settings(reads.aiGenerationLimit());
  }

  @Override
  @Transactional
  public Settings update(UpdateSettings command) {
    if (command.maxPerProblem() < MIN || command.maxPerProblem() > MAX) {
      throw new InvalidRequestException("PRB-001", "maxPerProblem must be between 1 and 10");
    }
    int old = settings.lockCap();
    if (old == command.maxPerProblem()) {
      return new Settings(old);
    }
    settings.updateCap(command.maxPerProblem(), command.actorId());
    try {
      audit.record(
          new CatalogAuditEntry(
              command.actorId(),
              ACTION,
              "PROBLEM_BANK_SETTINGS",
              "1",
              "{\"maxPerProblem\":" + old + "}",
              "{\"maxPerProblem\":" + command.maxPerProblem() + "}"));
    } catch (RuntimeException failure) {
      throw new ServiceUnavailableException(
          "PRB-050", "Audit write unavailable; change rolled back", failure);
    }
    return new Settings(command.maxPerProblem());
  }
}
