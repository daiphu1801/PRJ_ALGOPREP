package com.algoprep.bootstrap.aireview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.domain.exception.AiProviderUnavailableException;
import com.algoprep.aireview.domain.exception.AiSubmissionNotAcceptedException;
import com.algoprep.aireview.domain.exception.AiSubmissionNotFoundException;
import com.algoprep.aireview.domain.model.AiAuditEntry;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.identity.application.ports.in.RecordSystemAuditUseCase;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.judge.application.ports.in.GetAcceptedSubmissionSnapshotUseCase;
import com.algoprep.judge.domain.exception.JudgeReadUnavailableException;
import com.algoprep.judge.domain.exception.SubmissionNotAcceptedException;
import com.algoprep.judge.domain.exception.SubmissionNotFoundException;
import com.algoprep.judge.domain.model.AcceptedSubmissionSnapshot;
import com.algoprep.problembank.application.ports.in.ProblemBankStatsUseCase;
import com.algoprep.problembank.domain.model.ProblemBankStatsModels.ProblemStat;
import com.algoprep.problembank.domain.model.ProblemContextView;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Type translation and error mapping of the two F5 bridges (DD F5 section 1.3). */
class AiReviewBridgesTest {
  private final GetAcceptedSubmissionSnapshotUseCase judge =
      mock(GetAcceptedSubmissionSnapshotUseCase.class);
  private final SubmissionSnapshotBridge submissions = new SubmissionSnapshotBridge(judge);
  private final UUID id = UUID.randomUUID();

  @Test
  void aSnapshotKeepsOnlyTheFieldsF5Needs() {
    UUID user = UUID.randomUUID();
    UUID problem = UUID.randomUUID();
    when(judge.getAccepted(id))
        .thenReturn(new AcceptedSubmissionSnapshot(id, user, problem, "CPP", "int main(){}", 5, 6));
    var snapshot = submissions.getAccepted(id);
    assertThat(snapshot.userId()).isEqualTo(user);
    assertThat(snapshot.problemId()).isEqualTo(problem);
    assertThat(snapshot.language()).isEqualTo("CPP");
    assertThat(snapshot.sourceCode()).isEqualTo("int main(){}");
    assertThat(snapshot.toString()).doesNotContain("int main");
  }

  @Test
  void judgeErrorsMapToAirCodes() {
    doThrow(new SubmissionNotFoundException()).when(judge).getAccepted(id);
    assertThatThrownBy(() -> submissions.getAccepted(id))
        .isInstanceOf(AiSubmissionNotFoundException.class);
    doThrow(new SubmissionNotAcceptedException()).when(judge).getAccepted(id);
    assertThatThrownBy(() -> submissions.getAccepted(id))
        .isInstanceOf(AiSubmissionNotAcceptedException.class);
    doThrow(new JudgeReadUnavailableException()).when(judge).getAccepted(id);
    assertThatThrownBy(() -> submissions.getAccepted(id))
        .isInstanceOfSatisfying(
            AiProviderUnavailableException.class,
            e ->
                assertThat(e.reason())
                    .isEqualTo(AiProviderUnavailableException.Reason.UPSTREAM_PORT));
  }

  // The problem-bank port is wired by reflection: an incremental javac run in this module once
  // emitted an unqualified reference to the use case, so this test does not name the type.
  private ProblemContextBridge bridgeAnswering(Object answerOrFailure) throws Exception {
    Class<?> port =
        Class.forName("com.algoprep.problembank.application.ports.in.ReadProblemContextUseCase");
    Object proxy =
        Proxy.newProxyInstance(
            port.getClassLoader(),
            new Class<?>[] {port},
            (p, method, args) -> {
              if (answerOrFailure instanceof RuntimeException failure) {
                throw failure;
              }
              return answerOrFailure;
            });
    var constructor = ProblemContextBridge.class.getDeclaredConstructor(port);
    constructor.setAccessible(true);
    return constructor.newInstance(proxy);
  }

  @Test
  void aProblemContextIsTranslatedWithoutAnythingElse() throws Exception {
    var view =
        new ProblemContextView(
            "Two Sum",
            "statement",
            "constraints",
            List.of("array"),
            "Easy",
            new ProblemContextView.FunctionSignature("twoSum", "int[]", "[]"),
            null,
            "brief",
            true,
            false);
    var context = bridgeAnswering(view).getContext(id, "JAVA");
    assertThat(context.title()).isEqualTo("Two Sum");
    assertThat(context.functionSignature()).isEqualTo("twoSum([]) -> int[]");
    assertThat(context.aiBrief()).isEqualTo("brief");
    assertThat(context.noFullCode()).isTrue();
    assertThat(context.socraticOnly()).isFalse();
    assertThat(context.knownOptimalComplexity()).isNull();
  }

  @Test
  void aWithdrawnOrBrokenProblemIsAir501UpstreamPort() throws Exception {
    var withdrawn = bridgeAnswering(new ResourceNotFoundException("PRB-010", "x"));
    assertThatThrownBy(() -> withdrawn.getContext(id, "JAVA"))
        .isInstanceOfSatisfying(
            AiProviderUnavailableException.class,
            e ->
                assertThat(e.reason())
                    .isEqualTo(AiProviderUnavailableException.Reason.UPSTREAM_PORT));
    var broken = bridgeAnswering(new IllegalStateException("db down"));
    assertThatThrownBy(() -> broken.getContext(id, "JAVA"))
        .isInstanceOf(AiProviderUnavailableException.class);
  }

  @Test
  void problemTitlesAreTranslatedAndAFailureOrDeadlineIsOneError() {
    UUID p = UUID.randomUUID();
    var stats = mock(ProblemBankStatsUseCase.class);
    when(stats.problemSummaries(Set.of(p)))
        .thenReturn(
            List.of(new ProblemStat(p, "Two Sum", UUID.randomUUID(), "EASY", "Easy", false)));
    var bridge = new ProblemBankStatsBridge(stats, Duration.ofSeconds(2));
    var summaries = bridge.problemSummaries(Set.of(p));
    assertThat(summaries).hasSize(1);
    assertThat(summaries.get(0).title()).isEqualTo("Two Sum");
    assertThat(summaries.get(0).levelCode()).isEqualTo("EASY");

    doThrow(new RuntimeException("db down")).when(stats).problemSummaries(Set.of(p));
    assertThatThrownBy(() -> bridge.problemSummaries(Set.of(p)))
        .isInstanceOf(IllegalStateException.class);

    doAnswer(
            call -> {
              Thread.sleep(5_000);
              return List.of();
            })
        .when(stats)
        .problemSummaries(Set.of(p));
    var slow = new ProblemBankStatsBridge(stats, Duration.ofMillis(50));
    assertThatThrownBy(() -> slow.problemSummaries(Set.of(p)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void anAuditEntryIsHandedToIdentityUnchanged() {
    var identity = mock(RecordSystemAuditUseCase.class);
    UUID actor = UUID.randomUUID();
    new AiAuditBridge(identity)
        .record(new AiAuditEntry(actor, "AI_PROMPT_PUBLISHED", "PROMPT_TEMPLATE", "X", "{}", "{}"));
    verify(identity)
        .record(new AuditEntry(actor, "AI_PROMPT_PUBLISHED", "PROMPT_TEMPLATE", "X", "{}", "{}"));
  }
}
