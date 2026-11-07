package com.algoprep.problembank.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.problembank.application.dto.ProblemAuthoringQuery;
import com.algoprep.problembank.application.dto.ProblemAuthoringView;
import com.algoprep.problembank.application.policy.PublishChecklistPolicy;
import com.algoprep.problembank.application.policy.TestcaseViewMapper;
import com.algoprep.problembank.application.ports.out.TestcaseObjectStorePort;
import com.algoprep.problembank.application.ports.out.UserDirectoryPort;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord.AuthoringTestcase;
import com.algoprep.problembank.domain.model.ProblemContentData;
import com.algoprep.problembank.domain.model.ProblemSpecData;
import com.algoprep.problembank.domain.model.StoredProblemSpec;
import com.algoprep.problembank.domain.ports.out.ProblemAuthoringReadRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class GetProblemForAuthoringQueryHandlerTest {
  private final ProblemAuthoringReadRepository repository =
      mock(ProblemAuthoringReadRepository.class);
  private final TestcaseObjectStorePort store = mock(TestcaseObjectStorePort.class);
  private final UserDirectoryPort users = mock(UserDirectoryPort.class);
  private final MockEnvironment environment = new MockEnvironment();
  private final GetProblemForAuthoringQueryHandler handler =
      new GetProblemForAuthoringQueryHandler(
          repository,
          new TestcaseViewMapper(store),
          users,
          new PublishChecklistPolicy(environment));

  private final UUID problemId = UUID.randomUUID();
  private final UUID author = UUID.randomUUID();
  private final UUID other = UUID.randomUUID();

  @Test
  void aDraftOfSomeoneElseIsNotFoundAndNothingElseIsRead() {
    when(repository.find(problemId, false)).thenReturn(Optional.of(record("UNPUBLISHED", 0, 0, 0)));

    assertThatThrownBy(() -> handler.get(query(other, false, true)))
        .isInstanceOf(ResourceNotFoundException.class)
        .extracting(e -> ((ResourceNotFoundException) e).errorCode())
        .isEqualTo("PRB-010");
    verify(repository, never()).find(problemId, true);
  }

  @Test
  void aMissingOrDeletedProblemIsNotFound() {
    when(repository.find(problemId, false)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.get(query(author, false, true)))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void aPublishedProblemOfSomeoneElseIsReadOnlyWithoutBodiesOrReferenceSolution() {
    when(repository.find(problemId, false)).thenReturn(Optional.of(record("PUBLISHED", 3, 2, 1)));

    var view = handler.get(query(other, false, true));

    assertThat(view.readOnly()).isTrue();
    assertThat(view.referenceSolution()).isNull();
    assertThat(view.testcases()).isEmpty();
    assertThat(view.testcaseSummary())
        .isEqualTo(new ProblemAuthoringView.TestcaseSummary(3, 3, 2, 1));
    verify(repository, never()).find(problemId, true);
  }

  @Test
  void anAuthorWithoutTestcaseReadGetsOnlyTheSummaryAndNoBodyRead() {
    when(repository.find(problemId, false)).thenReturn(Optional.of(record("UNPUBLISHED", 2, 1, 1)));

    var view = handler.get(query(author, false, false));

    assertThat(view.readOnly()).isFalse();
    assertThat(view.referenceSolution()).isNotNull();
    assertThat(view.testcases()).isEmpty();
    assertThat(view.testcaseSummary().total()).isEqualTo(2);
    verify(repository, never()).find(problemId, true);
  }

  @Test
  void anAdminWithTestcaseReadSeesBodiesAndSignedUrlsForObjectStorage() {
    var inline =
        new AuthoringTestcase(
            UUID.randomUUID(),
            1,
            "SAMPLE",
            "MANUAL",
            true,
            null,
            "INLINE",
            "abé",
            null,
            "INLINE",
            "out",
            null);
    var big =
        new AuthoringTestcase(
            UUID.randomUUID(),
            2,
            "HIDDEN",
            "MANUAL",
            true,
            null,
            "MINIO",
            null,
            "k/in",
            "MINIO",
            null,
            "k/out");
    when(repository.find(problemId, false)).thenReturn(Optional.of(base("PUBLISHED", List.of())));
    when(repository.find(problemId, true))
        .thenReturn(Optional.of(base("PUBLISHED", List.of(inline, big))));
    when(store.head("k/in")).thenReturn(9_000L);
    when(store.head("k/out")).thenReturn(7L);
    when(store.presignGet("k/in", TestcaseViewMapper.DOWNLOAD_TTL)).thenReturn("u1");
    when(store.presignGet("k/out", TestcaseViewMapper.DOWNLOAD_TTL)).thenReturn("u2");

    var view = handler.get(query(other, true, true));

    assertThat(view.readOnly()).isFalse();
    var first = view.testcases().get(0);
    assertThat(first.input()).isEqualTo("abé");
    assertThat(first.inputSizeBytes()).isEqualTo(4); // UTF-8 bytes, not characters
    assertThat(first.inputDownloadUrl()).isNull();
    var second = view.testcases().get(1);
    assertThat(second.input()).isNull();
    assertThat(second.inputDownloadUrl()).isEqualTo("u1");
    assertThat(second.inputSizeBytes()).isEqualTo(9_000L);
    assertThat(second.expectedOutputDownloadUrl()).isEqualTo("u2");
  }

  @Test
  void checklistCountsOnlyApprovedCasesAndStopsAtTheThresholds() {
    var cases = new ArrayList<AuthoringTestcase>();
    for (int i = 0; i < 7; i++) {
      cases.add(row(i + 1, i < 2 ? "SAMPLE" : "HIDDEN", true));
    }
    cases.add(row(8, "HIDDEN", false)); // an unapproved AI draft is not counted
    when(repository.find(problemId, false)).thenReturn(Optional.of(base("UNPUBLISHED", cases)));

    var checklist = handler.get(query(author, false, false)).checklist();

    assertThat(checklist.testcaseCount()).isEqualTo(7);
    assertThat(checklist.sampleTestcaseCount()).isEqualTo(2);
    var byCode = new java.util.HashMap<String, ProblemAuthoringView.PublishChecklist.Item>();
    checklist.items().forEach(i -> byCode.put(i.code(), i));
    assertThat(byCode.get("MIN_APPROVED_TESTCASES").met()).isFalse(); // 7 < 8
    assertThat(byCode.get("MIN_APPROVED_TESTCASES").required()).isEqualTo(8);
    assertThat(byCode.get("MIN_APPROVED_SAMPLES").met()).isTrue(); // 2 >= 2
    assertThat(byCode.get("MIN_WORKED_EXAMPLES").met()).isTrue();
    assertThat(byCode.get("REFERENCE_SOLUTION_PASSES_ALL").met()).isNull();
    assertThat(checklist.sampleSolutionAllPass()).isNull();
  }

  @Test
  void thresholdsComeFromConfiguration() {
    environment.setProperty("algoprep.problem-bank.publish.min-approved-testcases", "7");
    var cases = new ArrayList<AuthoringTestcase>();
    for (int i = 0; i < 7; i++) {
      cases.add(row(i + 1, "SAMPLE", true));
    }
    when(repository.find(problemId, false)).thenReturn(Optional.of(base("UNPUBLISHED", cases)));

    var items = handler.get(query(author, false, false)).checklist().items();

    assertThat(items.get(0).met()).isTrue();
    assertThat(items.get(0).required()).isEqualTo(7);
  }

  @Test
  void specDeclaredNeedsBothFormatsAndASignatureUnlessTheWrapperIsOff() {
    when(repository.find(problemId, false))
        .thenReturn(Optional.of(withSpec(spec("in", "out", "f", "{}", "[]"), true)));
    assertThat(handler.get(query(author, false, false)).checklist().specDeclared()).isTrue();

    when(repository.find(problemId, false))
        .thenReturn(Optional.of(withSpec(spec("in", " ", "f", "{}", "[]"), true)));
    assertThat(handler.get(query(author, false, false)).checklist().specDeclared()).isFalse();

    when(repository.find(problemId, false))
        .thenReturn(Optional.of(withSpec(spec("in", "out", null, null, null), true)));
    assertThat(handler.get(query(author, false, false)).checklist().specDeclared()).isFalse();

    when(repository.find(problemId, false))
        .thenReturn(Optional.of(withSpec(spec("in", "out", null, null, null), false)));
    assertThat(handler.get(query(author, false, false)).checklist().specDeclared()).isTrue();

    when(repository.find(problemId, false)).thenReturn(Optional.of(withSpec(null, true)));
    var view = handler.get(query(author, false, false));
    assertThat(view.checklist().specDeclared()).isFalse();
    assertThat(view.spec()).isNull();
  }

  @Test
  void specIsRenderedWithItsVersion() {
    when(repository.find(problemId, false))
        .thenReturn(
            Optional.of(withSpec(spec("in", "out", "twoSum", "{\"kind\":\"INT\"}", "[]"), true)));

    var spec = handler.get(query(author, false, false)).spec();

    assertThat(spec.get("functionName").asString()).isEqualTo("twoSum");
    assertThat(spec.get("returnType").get("kind").asString()).isEqualTo("INT");
    assertThat(spec.get("specVersion").asInt()).isEqualTo(3);
    assertThat(spec.get("nameOverrides").isNull()).isTrue();
  }

  @Test
  void lastEditorNameIsNullWhenTheDirectoryFails() {
    var editor = UUID.randomUUID();
    when(repository.find(problemId, false))
        .thenReturn(Optional.of(base("UNPUBLISHED", List.of(), editor)));
    when(users.getDisplayNames(any())).thenThrow(new IllegalStateException("down"));

    var view = handler.get(query(author, false, false));

    assertThat(view.updatedBy().userId()).isEqualTo(editor);
    assertThat(view.updatedBy().displayName()).isNull();
  }

  @Test
  void lastEditorNameComesFromTheDirectoryAndAbsentEditorIsNull() {
    var editor = UUID.randomUUID();
    when(repository.find(problemId, false))
        .thenReturn(Optional.of(base("UNPUBLISHED", List.of(), editor)));
    when(users.getDisplayNames(any())).thenReturn(Map.of(editor, "Tran Thi Giang"));
    assertThat(handler.get(query(author, false, false)).updatedBy().displayName())
        .isEqualTo("Tran Thi Giang");

    when(repository.find(problemId, false))
        .thenReturn(Optional.of(base("UNPUBLISHED", List.of(), null)));
    assertThat(handler.get(query(author, false, false)).updatedBy()).isNull();
  }

  @Test
  void aiGenerationShowsUsedAndTheConfiguredLimit() {
    when(repository.find(problemId, false)).thenReturn(Optional.of(record("UNPUBLISHED", 0, 0, 0)));
    when(repository.aiGenerationLimit()).thenReturn(4);

    var ai = handler.get(query(author, false, false)).aiGeneration();

    assertThat(ai.limit()).isEqualTo(4);
    assertThat(ai.used()).isEqualTo(1);
  }

  private ProblemAuthoringQuery query(UUID actor, boolean admin, boolean testcases) {
    return new ProblemAuthoringQuery(problemId, actor, admin, testcases);
  }

  private AuthoringTestcase row(int position, String visibility, boolean approved) {
    return new AuthoringTestcase(
        UUID.randomUUID(),
        position,
        visibility,
        "MANUAL",
        approved,
        null,
        "INLINE",
        null,
        null,
        "INLINE",
        null,
        null);
  }

  private AuthoringProblemRecord record(String status, int total, int samples, int hidden) {
    var cases = new ArrayList<AuthoringTestcase>();
    for (int i = 0; i < samples; i++) {
      cases.add(row(cases.size() + 1, "SAMPLE", true));
    }
    for (int i = 0; i < hidden; i++) {
      cases.add(row(cases.size() + 1, "HIDDEN", true));
    }
    while (cases.size() < total) {
      cases.add(row(cases.size() + 1, "HIDDEN", true));
    }
    return base(status, cases);
  }

  private AuthoringProblemRecord base(String status, List<AuthoringTestcase> cases) {
    return base(status, cases, null);
  }

  private AuthoringProblemRecord base(
      String status, List<AuthoringTestcase> cases, UUID updatedBy) {
    return build(status, cases, updatedBy, null, true);
  }

  private AuthoringProblemRecord withSpec(StoredProblemSpec spec, boolean wrapper) {
    return build("UNPUBLISHED", List.of(), null, spec, wrapper);
  }

  private StoredProblemSpec spec(
      String in, String out, String function, String returnType, String parameters) {
    return new StoredProblemSpec(
        new ProblemSpecData("EXACT", null, in, out, function, returnType, parameters, null), 3);
  }

  private AuthoringProblemRecord build(
      String status,
      List<AuthoringTestcase> cases,
      UUID updatedBy,
      StoredProblemSpec spec,
      boolean wrapper) {
    return new AuthoringProblemRecord(
        problemId,
        "two-sum",
        "Two Sum",
        "statement",
        "",
        UUID.randomUUID(),
        status,
        wrapper,
        new ProblemContentData.Limits(1000, 256, 64, null),
        1,
        1,
        null,
        true,
        true,
        author,
        updatedBy,
        null,
        Instant.parse("2026-11-05T00:00:00Z"),
        null,
        List.of(),
        List.of(),
        new ProblemContentData.Solution("PYTHON", "print(1)"),
        List.of(
            new ProblemContentData.Example(UUID.randomUUID(), 1, "1", "1", null),
            new ProblemContentData.Example(UUID.randomUUID(), 2, "2", "2", null)),
        cases,
        spec);
  }
}
