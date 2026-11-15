package com.algoprep.problembank.application.command;

import com.algoprep.common.exception.PermissionDeniedException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.problembank.application.dto.SaveAiContextCommand;
import com.algoprep.problembank.application.ports.in.SaveAiAuthoringContextUseCase;
import com.algoprep.problembank.domain.exception.ProblemVersionConflictException;
import com.algoprep.problembank.domain.ports.out.ProblemContentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DD F2 section 39: three columns of the problem row. It never touches {@code spec_version} or the
 * testcase-set version, and the text is not normalised beyond trimming: isolating it from the
 * system instruction is the prompt builder's job in F5.
 */
@Service
class SaveAiAuthoringContextCommandHandler implements SaveAiAuthoringContextUseCase {
  static final int MAX_BRIEF_CHARS = 4000;

  private final ProblemContentRepository repository;

  SaveAiAuthoringContextCommandHandler(ProblemContentRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public SaveAiContextCommand.Result save(SaveAiContextCommand command) {
    var old =
        repository
            .lock(command.problemId())
            .orElseThrow(SaveAiAuthoringContextCommandHandler::missing);
    if (!command.admin() && !old.authorId().equals(command.actorId())) {
      if (!"PUBLISHED".equals(old.status())) {
        throw missing();
      }
      throw new PermissionDeniedException("PRB-003", "Only the author can modify this problem");
    }
    var brief = brief(command.briefText());
    if (command.noFullCode() == null) {
      throw ProblemContentValidation.invalid("noFullCode", "required");
    }
    if (command.questionOnly() == null) {
      throw ProblemContentValidation.invalid("questionOnly", "required");
    }
    if (command.expectedUpdatedAt() == null) {
      throw ProblemContentValidation.invalid("expectedUpdatedAt", "required");
    }
    if (!old.updatedAt().equals(command.expectedUpdatedAt())) {
      throw new ProblemVersionConflictException(old.updatedAt());
    }
    var updated =
        repository.writeAiContext(
            command.problemId(),
            command.actorId(),
            brief,
            command.noFullCode(),
            command.questionOnly());
    return new SaveAiContextCommand.Result(
        brief, command.noFullCode(), command.questionOnly(), updated);
  }

  /** Blank after trimming is stored as null, otherwise verbatim; the limit counts code points. */
  private static String brief(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    if (raw.codePointCount(0, raw.length()) > MAX_BRIEF_CHARS) {
      throw ProblemContentValidation.invalid("briefText", "maxLength");
    }
    if (raw.indexOf(0) >= 0) {
      throw ProblemContentValidation.invalid("briefText", "text");
    }
    return raw;
  }

  private static ResourceNotFoundException missing() {
    return new ResourceNotFoundException("PRB-010", "Problem not found");
  }
}
