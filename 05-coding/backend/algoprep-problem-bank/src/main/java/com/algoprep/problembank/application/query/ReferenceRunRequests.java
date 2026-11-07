package com.algoprep.problembank.application.query;

import com.algoprep.problembank.application.dto.ReferenceSolutionRun;
import com.algoprep.problembank.application.ports.out.JudgeConfigReadPort;
import com.algoprep.problembank.application.ports.out.TestcaseObjectStorePort;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord.AuthoringTestcase;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Builds the sandbox request of a reference run: testcase bodies (a MINIO side is read from the
 * store), the problem limits and the compile timeout. The same request the publish gate sends.
 */
@Component
class ReferenceRunRequests {
  private final TestcaseObjectStorePort store;
  private final JudgeConfigReadPort config;

  ReferenceRunRequests(TestcaseObjectStorePort store, JudgeConfigReadPort config) {
    this.store = store;
    this.config = config;
  }

  ReferenceSolutionRun.Request build(
      AuthoringProblemRecord problem, String language, String source, List<AuthoringTestcase> run) {
    var limits = problem.limits();
    var cases =
        run.stream()
            .map(
                t ->
                    new ReferenceSolutionRun.Case(
                        t.id(),
                        text(t.inputStorage(), t.inputInline(), t.inputObjectKey()),
                        text(t.expectedStorage(), t.expectedInline(), t.expectedObjectKey())))
            .toList();
    return new ReferenceSolutionRun.Request(
        problem.id(),
        language,
        problem.functionWrapperSupported(),
        source,
        problem.spec() == null ? 1 : problem.spec().specVersion(),
        new ReferenceSolutionRun.Limits(
            limits.timeLimitMs(),
            limits.memoryLimitMb(),
            limits.maxOutputSizeKb(),
            config.getGradingDefaults().compileTimeoutSeconds()),
        cases);
  }

  private String text(String storage, String inline, String objectKey) {
    if ("INLINE".equals(storage)) {
      return inline;
    }
    return new String(store.get(objectKey), StandardCharsets.UTF_8);
  }
}
