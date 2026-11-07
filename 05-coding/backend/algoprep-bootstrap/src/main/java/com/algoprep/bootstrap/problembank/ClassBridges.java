package com.algoprep.bootstrap.problembank;

import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.identity.application.ports.in.ListInstructorClassesUseCase;
import com.algoprep.judge.application.dto.AssignedProblemMetricsQuery;
import com.algoprep.judge.application.ports.in.GetAssignedProblemMetricsUseCase;
import com.algoprep.judge.domain.model.AssignmentPeriod;
import com.algoprep.problembank.application.ports.out.ClassProblemMetricsPort;
import com.algoprep.problembank.application.ports.out.InstructorClassPort;
import com.algoprep.problembank.domain.model.ClassAssignmentModels.ClassRef;
import com.algoprep.problembank.domain.model.ClassAssignmentModels.Metric;
import com.algoprep.problembank.domain.model.ClassAssignmentSnapshot;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.stereotype.Component;

/**
 * Type translation between problem-bank and its two neighbours, each read on its own virtual thread
 * with a deadline (DD F2 58.2): identity 2 seconds, judge 3 seconds.
 */
@Component
public final class ClassBridges implements InstructorClassPort, ClassProblemMetricsPort {
  private final ListInstructorClassesUseCase instructorClasses;
  private final GetAssignedProblemMetricsUseCase judgeMetrics;

  public ClassBridges(
      ListInstructorClassesUseCase instructorClasses,
      GetAssignedProblemMetricsUseCase judgeMetrics) {
    this.instructorClasses = instructorClasses;
    this.judgeMetrics = judgeMetrics;
  }

  @Override
  public List<ClassRef> classesOf(UUID instructorId) {
    try {
      return within(
          Duration.ofSeconds(2),
          () ->
              instructorClasses.list(instructorId).stream()
                  .map(c -> new ClassRef(c.id(), c.name()))
                  .toList());
    } catch (RuntimeException
        | InterruptedException
        | ExecutionException
        | TimeoutException failure) {
      if (failure instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new ServiceUnavailableException("PRB-058", "Class information is unavailable", null);
    }
  }

  @Override
  public Optional<Map<UUID, Metric>> read(
      UUID instructorId,
      Set<UUID> classIds,
      Set<UUID> problemIds,
      List<ClassAssignmentSnapshot> history) {
    try {
      var periods =
          history.stream()
              .map(
                  h ->
                      new AssignmentPeriod(
                          h.classId(), h.problemId(), h.assignedAt(), h.removedAt()))
              .toList();
      var found =
          within(
              Duration.ofSeconds(3),
              () ->
                  judgeMetrics.read(
                      new AssignedProblemMetricsQuery(
                          instructorId, classIds, problemIds, periods)));
      var result = new HashMap<UUID, Metric>();
      found.forEach((id, m) -> result.put(id, new Metric(m.submissionCount(), m.acRatePercent())));
      return Optional.of(result);
    } catch (RuntimeException
        | InterruptedException
        | ExecutionException
        | TimeoutException failure) {
      if (failure instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      return Optional.empty();
    }
  }

  private static <T> T within(Duration limit, Callable<T> work)
      throws InterruptedException, ExecutionException, TimeoutException {
    var task = new FutureTask<>(work);
    Thread.ofVirtual().name("class-bridge-read").start(task);
    try {
      return task.get(limit.toNanos(), TimeUnit.NANOSECONDS);
    } finally {
      task.cancel(true);
    }
  }
}
