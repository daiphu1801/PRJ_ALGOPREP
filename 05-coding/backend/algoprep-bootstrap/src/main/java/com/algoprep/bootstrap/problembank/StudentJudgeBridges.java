package com.algoprep.bootstrap.problembank;

import com.algoprep.identity.application.ports.in.ListMyClassesUseCase;
import com.algoprep.judge.application.ports.in.GetMyClassAssignmentStatesUseCase;
import com.algoprep.judge.application.ports.in.GetMyTopicSubmissionMetricsUseCase;
import com.algoprep.judge.domain.model.AssignmentPeriod;
import com.algoprep.problembank.application.ports.out.MyClassAssignmentStatePort;
import com.algoprep.problembank.application.ports.out.MyTopicSubmissionMetricsPort;
import com.algoprep.problembank.application.ports.out.StudentEnrollmentPort;
import com.algoprep.problembank.domain.model.ClassAssignmentModels.ClassRef;
import com.algoprep.problembank.domain.model.StudentClassModels.AssignmentKey;
import com.algoprep.problembank.domain.model.StudentClassModels.ClassSolveState;
import com.algoprep.problembank.domain.model.SuggestedProblemModels.TopicMetric;
import java.time.Duration;
import java.util.Collection;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.concurrent.DelegatingSecurityContextCallable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Type translation between problem-bank and the two neighbours of the learner's screens (DD F2
 * 58.2): identity answers the enrolment (2 seconds), the judge module answers the per-assignment
 * state P7 and the per-topic counts P5 (3 seconds each). Every read runs on its own virtual thread
 * with a deadline and any failure is an absent answer, which the caller turns into a degraded
 * response instead of a made-up number. The judge reads take the student from the security context,
 * so the context of the request is handed to the worker thread.
 */
@Component
public final class StudentJudgeBridges
    implements StudentEnrollmentPort, MyClassAssignmentStatePort, MyTopicSubmissionMetricsPort {
  private final ListMyClassesUseCase classes;
  private final GetMyClassAssignmentStatesUseCase assignmentStates;
  private final GetMyTopicSubmissionMetricsUseCase topicMetrics;
  private final Duration identityLimit;
  private final Duration judgeLimit;

  @Autowired
  public StudentJudgeBridges(
      ListMyClassesUseCase classes,
      GetMyClassAssignmentStatesUseCase assignmentStates,
      GetMyTopicSubmissionMetricsUseCase topicMetrics) {
    this(classes, assignmentStates, topicMetrics, Duration.ofSeconds(2), Duration.ofSeconds(3));
  }

  StudentJudgeBridges(
      ListMyClassesUseCase classes,
      GetMyClassAssignmentStatesUseCase assignmentStates,
      GetMyTopicSubmissionMetricsUseCase topicMetrics,
      Duration identityLimit,
      Duration judgeLimit) {
    this.classes = classes;
    this.assignmentStates = assignmentStates;
    this.topicMetrics = topicMetrics;
    this.identityLimit = identityLimit;
    this.judgeLimit = judgeLimit;
  }

  @Override
  public Optional<List<ClassRef>> activeClassesOf(UUID studentId) {
    return within(
        identityLimit,
        () ->
            classes.list(studentId).stream()
                .map(c -> new ClassRef(c.classId(), c.name()))
                .toList());
  }

  @Override
  public Optional<Map<AssignmentKey, ClassSolveState>> read(Collection<AssignmentKey> assignments) {
    var periods =
        assignments.stream()
            .map(a -> new AssignmentPeriod(a.classId(), a.problemId(), a.assignedAt(), null))
            .toList();
    return within(
        judgeLimit,
        () -> {
          var found = assignmentStates.read(periods);
          var result = new HashMap<AssignmentKey, ClassSolveState>();
          found.forEach(
              (k, v) ->
                  result.put(
                      new AssignmentKey(k.classId(), k.problemId(), k.assignedAt()),
                      ClassSolveState.valueOf(v.name())));
          return result;
        });
  }

  @Override
  public Optional<Map<UUID, TopicMetric>> read(Map<UUID, Set<UUID>> topicProblemIds) {
    return within(
        judgeLimit,
        () -> {
          var result = new HashMap<UUID, TopicMetric>();
          topicMetrics
              .readTopics(topicProblemIds)
              .forEach(
                  (topic, r) ->
                      result.put(
                          topic,
                          new TopicMetric(r.submissionCount(), r.acceptedCount(), r.acRate())));
          return result;
        });
  }

  private static <T> Optional<T> within(Duration limit, Callable<T> work) {
    var task =
        new FutureTask<>(
            DelegatingSecurityContextCallable.create(work, SecurityContextHolder.getContext()));
    Thread.ofVirtual().name("student-judge-bridge").start(task);
    try {
      return Optional.of(task.get(limit.toNanos(), TimeUnit.NANOSECONDS));
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      return Optional.empty();
    } catch (ExecutionException | TimeoutException failure) {
      return Optional.empty();
    } finally {
      task.cancel(true);
    }
  }
}
