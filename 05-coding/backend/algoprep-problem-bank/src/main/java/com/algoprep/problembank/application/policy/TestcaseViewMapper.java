package com.algoprep.problembank.application.policy;

import com.algoprep.problembank.application.dto.ProblemAuthoringView.TestcaseView;
import com.algoprep.problembank.application.ports.out.TestcaseObjectStorePort;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord.AuthoringTestcase;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.stereotype.Component;

/** The author's {@code TestcaseAuthorDto}; a large side is a signed link, never the body. */
@Component
public final class TestcaseViewMapper {
  public static final Duration DOWNLOAD_TTL = Duration.ofMinutes(5);

  private final TestcaseObjectStorePort store;

  public TestcaseViewMapper(TestcaseObjectStorePort store) {
    this.store = store;
  }

  public TestcaseView view(AuthoringTestcase t) {
    boolean inputInline = "INLINE".equals(t.inputStorage());
    boolean outputInline = "INLINE".equals(t.expectedStorage());
    return new TestcaseView(
        t.id(),
        t.position(),
        t.visibility(),
        t.origin(),
        t.approved(),
        t.category(),
        inputInline ? t.inputInline() : null,
        t.inputStorage(),
        inputInline ? bytes(t.inputInline()) : store.head(t.inputObjectKey()),
        inputInline ? null : store.presignGet(t.inputObjectKey(), DOWNLOAD_TTL),
        outputInline ? t.expectedInline() : null,
        t.expectedStorage(),
        outputInline ? bytes(t.expectedInline()) : store.head(t.expectedObjectKey()),
        outputInline ? null : store.presignGet(t.expectedObjectKey(), DOWNLOAD_TTL));
  }

  private static long bytes(String value) {
    return value == null ? 0 : value.getBytes(StandardCharsets.UTF_8).length;
  }
}
