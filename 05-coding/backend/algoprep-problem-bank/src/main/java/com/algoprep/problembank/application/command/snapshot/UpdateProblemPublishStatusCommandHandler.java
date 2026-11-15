package com.algoprep.problembank.application.command;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.exception.PermissionDeniedException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.problembank.application.dto.ProblemAuthoringView.PublishChecklist;
import com.algoprep.problembank.application.dto.UpdateProblemStatusCommand;
import com.algoprep.problembank.application.dto.UpdateProblemStatusResult;
import com.algoprep.problembank.application.ports.in.PublishTestcaseSnapshotUseCase;
import com.algoprep.problembank.application.ports.in.UpdateProblemPublishStatusUseCase;
import com.algoprep.problembank.domain.exception.ProblemVersionConflictException;
import com.algoprep.problembank.domain.exception.PublishChecklistFailedException;
import com.algoprep.problembank.domain.model.ProblemContentSnapshot;
import com.algoprep.problembank.domain.model.ProblemStatusWrite;
import com.algoprep.problembank.domain.ports.out.ProblemAuthoringReadRepository;
import com.algoprep.problembank.domain.ports.out.ProblemContentRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DD F2 section 40. The parent row stays locked for the whole call, the sandbox run included.
 * ponytail: holding a row lock across the run (bounded by the executor's own deadline) keeps the
 * checklist and the status change consistent; move the run before the lock if authors start to wait
 * on each other.
 */
@Service
class UpdateProblemPublishStatusCommandHandler implements UpdateProblemPublishStatusUseCase {
  private final ProblemContentRepository content;
  private final ProblemAuthoringReadRepository reads;
  private final PublishTestcaseSnapshotUseCase snapshots;
  private final PublishGate gate;
  private final StatusChangeRecorder recorder;

  UpdateProblemPublishStatusCommandHandler(
      ProblemContentRepository content,
      ProblemAuthoringReadRepository reads,
      PublishTestcaseSnapshotUseCase snapshots,
      PublishGate gate,
      StatusChangeRecorder recorder) {
    this.content = content;
    this.reads = reads;
    this.snapshots = snapshots;
    this.gate = gate;
    this.recorder = recorder;
  }

  @Override
  @Transactional
  public UpdateProblemStatusResult update(UpdateProblemStatusCommand command) {
    boolean publish = "PUBLISHED".equals(command.status());
    if (!publish && !"UNPUBLISHED".equals(command.status())) {
      throw new InvalidRequestException("PRB-001", "Invalid status");
    }
    var old = authorize(command);
    var problem =
        reads
            .find(command.problemId(), false)
            .orElseThrow(UpdateProblemPublishStatusCommandHandler::missing);
    if (old.status().equals(command.status())) {
      return new UpdateProblemStatusResult(
          old.status(),
          problem.publishedAt(),
          problem.currentTestcaseSetVersion(),
          gate.counting(problem),
          old.updatedAt());
    }
    if (!publish) {
      return result("UNPUBLISHED", recorder.change(command, false), gate.counting(problem));
    }
    var checklist = gate.evaluate(problem);
    var missing = missing(checklist);
    if (!missing.isEmpty()) {
      throw new PublishChecklistFailedException(missing);
    }
    snapshots.publishSnapshot(command.problemId());
    return result("PUBLISHED", recorder.change(command, true), checklist);
  }

  private ProblemContentSnapshot authorize(UpdateProblemStatusCommand command) {
    var old =
        content
            .lock(command.problemId())
            .orElseThrow(UpdateProblemPublishStatusCommandHandler::missing);
    if (!command.admin() && !old.authorId().equals(command.actorId())) {
      if (!"PUBLISHED".equals(old.status())) {
        throw missing();
      }
      throw new PermissionDeniedException("PRB-003", "Only the author can modify this problem");
    }
    if (command.expectedUpdatedAt() == null) {
      throw new InvalidRequestException("PRB-001", "Missing expectedUpdatedAt");
    }
    if (!old.updatedAt().equals(command.expectedUpdatedAt())) {
      throw new ProblemVersionConflictException(old.updatedAt());
    }
    return old;
  }

  private static List<Map<String, Object>> missing(PublishChecklist checklist) {
    var items = new ArrayList<Map<String, Object>>();
    for (var item : checklist.items()) {
      if (!Boolean.TRUE.equals(item.met())) {
        var entry = new LinkedHashMap<String, Object>();
        entry.put("code", item.code());
        entry.put("met", false);
        entry.put("required", item.required());
        entry.put("actual", item.actual());
        items.add(entry);
      }
    }
    return items;
  }

  private static UpdateProblemStatusResult result(
      String status, ProblemStatusWrite write, PublishChecklist checklist) {
    return new UpdateProblemStatusResult(
        status,
        write.publishedAt(),
        write.currentTestcaseSetVersion(),
        checklist,
        write.updatedAt());
  }

  private static ResourceNotFoundException missing() {
    return new ResourceNotFoundException("PRB-010", "Problem not found");
  }
}
