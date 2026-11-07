package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.identity.application.ports.in.ListMyClassesUseCase;
import com.algoprep.identity.domain.model.MyClass;
import com.algoprep.judge.application.dto.MySubmissionMetrics.TopicRatio;
import com.algoprep.judge.application.ports.in.GetMyClassAssignmentStatesUseCase;
import com.algoprep.judge.application.ports.in.GetMyTopicSubmissionMetricsUseCase;
import com.algoprep.judge.domain.model.AssignmentPeriod;
import com.algoprep.judge.domain.model.AssignmentSolveState;
import com.algoprep.problembank.domain.model.StudentClassModels.AssignmentKey;
import com.algoprep.problembank.domain.model.StudentClassModels.ClassSolveState;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

class StudentJudgeBridgesTest {
  private final ListMyClassesUseCase classes = mock(ListMyClassesUseCase.class);
  private final GetMyClassAssignmentStatesUseCase states =
      mock(GetMyClassAssignmentStatesUseCase.class);
  private final GetMyTopicSubmissionMetricsUseCase topics =
      mock(GetMyTopicSubmissionMetricsUseCase.class);
  private final StudentJudgeBridges bridges = new StudentJudgeBridges(classes, states, topics);
  private final UUID student = UUID.randomUUID();

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void enrolmentIsTranslatedToClassReferences() {
    var id = UUID.randomUUID();
    when(classes.list(student))
        .thenReturn(List.of(new MyClass(id, "Algo", "", null, "T", Instant.now())));

    var found = bridges.activeClassesOf(student).orElseThrow();

    assertThat(found)
        .singleElement()
        .satisfies(
            c -> {
              assertThat(c.id()).isEqualTo(id);
              assertThat(c.name()).isEqualTo("Algo");
            });
  }

  @Test
  void aFailingOrSlowIdentityReadIsAnAbsentAnswer() {
    when(classes.list(student)).thenThrow(new IllegalStateException("down"));
    assertThat(bridges.activeClassesOf(student)).isEmpty();

    var slow =
        new StudentJudgeBridges(
            classes, states, topics, Duration.ofMillis(50), Duration.ofMillis(50));
    doAnswer(
            call -> {
              Thread.sleep(2000);
              return List.of();
            })
        .when(classes)
        .list(student);
    assertThat(slow.activeClassesOf(student)).isEmpty();
  }

  @Test
  void assignmentStatesAreSentAsOpenPeriodsAndMappedBackByKey() {
    var key =
        new AssignmentKey(
            UUID.randomUUID(), UUID.randomUUID(), Instant.parse("2026-11-01T00:00:00Z"));
    var sent = new AtomicReference<List<AssignmentPeriod>>();
    when(states.read(any()))
        .thenAnswer(
            call -> {
              List<AssignmentPeriod> periods = call.getArgument(0);
              sent.set(periods);
              return Map.of(periods.get(0).key(), AssignmentSolveState.ATTEMPTED);
            });

    var found = bridges.read(List.of(key)).orElseThrow();

    assertThat(found).containsEntry(key, ClassSolveState.ATTEMPTED);
    assertThat(sent.get())
        .singleElement()
        .satisfies(
            p -> {
              assertThat(p.removedAt()).isNull();
              assertThat(p.assignedAt()).isEqualTo(key.assignedAt());
            });
  }

  @Test
  void theSecurityContextOfTheRequestReachesTheJudgeWorkerThread() {
    Authentication caller = new TestingAuthenticationToken("student", "n/a", "ROLE_STUDENT");
    SecurityContextHolder.setContext(new SecurityContextImpl(caller));
    var seen = new AtomicReference<Authentication>();
    when(states.read(any()))
        .thenAnswer(
            call -> {
              seen.set(SecurityContextHolder.getContext().getAuthentication());
              return Map.of();
            });

    bridges.read(List.of());

    assertThat(seen.get()).isSameAs(caller);
  }

  @Test
  void aJudgeFailureIsAnAbsentAnswerForBothReads() {
    when(states.read(any())).thenThrow(new IllegalStateException("down"));
    when(topics.readTopics(any())).thenThrow(new IllegalStateException("down"));

    assertThat(bridges.read(List.of())).isEmpty();
    assertThat(bridges.read(Map.<UUID, Set<UUID>>of())).isEmpty();
  }

  @Test
  void topicRatiosAreTranslatedToTopicMetrics() {
    var topic = UUID.randomUUID();
    when(topics.readTopics(any())).thenReturn(Map.of(topic, new TopicRatio(4, 1, 0.25)));

    var found = bridges.read(Map.of(topic, Set.of(UUID.randomUUID()))).orElseThrow();

    assertThat(found.get(topic).submissionCount()).isEqualTo(4);
    assertThat(found.get(topic).acceptedCount()).isEqualTo(1);
    assertThat(found.get(topic).acRate()).isEqualTo(0.25);
  }
}
