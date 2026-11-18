package com.algoprep.problembank.presentation.controller;

import static com.algoprep.problembank.presentation.controller.ControllerSupport.jwt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.problembank.application.dto.AuthoringScope;
import com.algoprep.problembank.application.dto.generation.TestcaseGeneration;
import com.algoprep.problembank.application.dto.grading.ReferenceRunReport;
import com.algoprep.problembank.application.dto.problem.AuthoringDefaults;
import com.algoprep.problembank.application.dto.testcase.BulkTestcaseUpload;
import com.algoprep.problembank.application.dto.testcase.TestcaseCommands;
import com.algoprep.problembank.application.ports.in.generation.AiGenerationSettingsUseCase;
import com.algoprep.problembank.application.ports.in.generation.GenerateTestcasesUseCase;
import com.algoprep.problembank.application.ports.in.grading.RunReferenceSolutionUseCase;
import com.algoprep.problembank.application.ports.in.problem.ReadAuthoringDefaultsUseCase;
import com.algoprep.problembank.application.ports.in.testcase.AuthorTestcasesUseCase;
import com.algoprep.problembank.application.ports.in.testcase.BulkTestcaseUploadUseCase;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

/** Testcase authoring, bulk upload, generation and AI settings endpoints (DD F2 41 to 49). */
class TestcaseControllersTest {
  private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
  private static final UUID PROBLEM = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID TC = UUID.fromString("00000000-0000-0000-0000-000000000002");
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final AuthorTestcasesUseCase authoring = mock(AuthorTestcasesUseCase.class);
  private final ProblemTestcaseController controller = new ProblemTestcaseController(authoring);

  // ---- inline authoring ----

  @Test
  void createMapsEveryBodyFieldIntoAScopedCommand() {
    controller.create(
        jwt(ACTOR, "ROLE_ADMIN"),
        PROBLEM.toString(),
        new ProblemTestcaseController.CreateRequest("SAMPLE", "1", "2", "EXTREME"));

    var command = ArgumentCaptor.forClass(TestcaseCommands.Create.class);
    verify(authoring).create(command.capture());
    assertThat(command.getValue().scope()).isEqualTo(new AuthoringScope(ACTOR, true));
    assertThat(command.getValue().problemId()).isEqualTo(PROBLEM);
    assertThat(command.getValue().visibility()).isEqualTo("SAMPLE");
    assertThat(command.getValue().expectedOutput()).isEqualTo("2");
    assertThat(command.getValue().category()).isEqualTo("EXTREME");
  }

  @Test
  void malformedIdsGiveDistinctNotFoundCodesForProblemAndTestcase() {
    var caller = jwt(ACTOR, "TESTCASE_MANAGEMENT:UPDATE");

    assertThatThrownBy(
            () ->
                controller.create(
                    caller,
                    "zzz",
                    new ProblemTestcaseController.CreateRequest(null, "1", "2", null)))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Problem");
    assertThatThrownBy(() -> controller.delete(caller, PROBLEM.toString(), "zzz"))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Testcase");
    assertThatThrownBy(
            () -> controller.update(caller, PROBLEM.toString(), "zzz", JSON.readTree("{}")))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Testcase");
  }

  @Test
  void updateChangesOnlyTheKeysPresentInThePatchBody() {
    controller.update(
        jwt(ACTOR, "TESTCASE_MANAGEMENT:UPDATE"),
        PROBLEM.toString(),
        TC.toString(),
        JSON.readTree("{\"visibility\":\"HIDDEN\",\"category\":null,\"approved\":true}"));

    var command = ArgumentCaptor.forClass(TestcaseCommands.Update.class);
    verify(authoring).update(command.capture());
    var patch = command.getValue().patch();
    assertThat(patch.visibility()).isEqualTo("HIDDEN");
    assertThat(patch.input()).isNull();
    assertThat(patch.categoryPresent()).isTrue();
    assertThat(patch.category()).isNull();
    assertThat(patch.approved()).isTrue();
  }

  @Test
  void updateWithoutCategoryKeyLeavesCategoryUntouched() {
    controller.update(
        jwt(ACTOR),
        PROBLEM.toString(),
        TC.toString(),
        JSON.readTree("{\"input\":\"x\",\"expectedOutput\":\"y\"}"));

    var command = ArgumentCaptor.forClass(TestcaseCommands.Update.class);
    verify(authoring).update(command.capture());
    assertThat(command.getValue().patch().categoryPresent()).isFalse();
    assertThat(command.getValue().patch().approved()).isNull();
    assertThat(command.getValue().patch().expectedOutput()).isEqualTo("y");
  }

  @Test
  void updateRejectsWronglyTypedValuesAndNonObjectBodiesWithPrb001() {
    var caller = jwt(ACTOR);
    String id = PROBLEM.toString();
    String tc = TC.toString();
    for (String bad :
        new String[] {"{\"visibility\":5}", "{\"approved\":\"yes\"}", "[]", "\"text\"", "5"}) {
      assertThatThrownBy(() -> controller.update(caller, id, tc, JSON.readTree(bad)))
          .as(bad)
          .isInstanceOf(InvalidRequestException.class);
    }
    assertThatThrownBy(() -> controller.update(caller, id, tc, null))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  void deleteAndReorderForwardIds() {
    controller.delete(jwt(ACTOR), PROBLEM.toString(), TC.toString());
    controller.reorder(
        jwt(ACTOR), PROBLEM.toString(), new ProblemTestcaseController.OrderRequest(List.of(TC)));

    verify(authoring)
        .delete(new TestcaseCommands.Delete(new AuthoringScope(ACTOR, false), PROBLEM, TC));
    verify(authoring)
        .reorder(
            new TestcaseCommands.Reorder(new AuthoringScope(ACTOR, false), PROBLEM, List.of(TC)));
  }

  // ---- bulk upload and reference run ----

  private final BulkTestcaseUploadUseCase uploads = mock(BulkTestcaseUploadUseCase.class);
  private final RunReferenceSolutionUseCase reference = mock(RunReferenceSolutionUseCase.class);
  private final ProblemTestcaseBulkController bulk =
      new ProblemTestcaseBulkController(uploads, reference);

  @Test
  void requestUrlsDefaultsMissingSizesToZeroAndPassesNullItemsThrough() {
    bulk.requestUrls(
        jwt(ACTOR),
        PROBLEM.toString(),
        new ProblemTestcaseBulkController.UploadUrlRequest(
            java.util.Arrays.asList(
                new ProblemTestcaseBulkController.PairRequest("a", 10L, null, "SAMPLE", "TYPICAL"),
                null)));
    bulk.requestUrls(
        jwt(ACTOR), PROBLEM.toString(), new ProblemTestcaseBulkController.UploadUrlRequest(null));

    var command = ArgumentCaptor.forClass(BulkTestcaseUpload.RequestUrls.class);
    verify(uploads, org.mockito.Mockito.times(2)).requestUrls(command.capture());
    var first = command.getAllValues().get(0);
    assertThat(first.items().get(0).inputSizeBytes()).isEqualTo(10);
    assertThat(first.items().get(0).expectedOutputSizeBytes()).isZero();
    assertThat(first.items().get(1)).isNull();
    assertThat(command.getAllValues().get(1).items()).isNull();
  }

  @Test
  void confirmTreatsAMalformedUploadIdAsAnUnknownSessionPrb044() {
    var caller = jwt(ACTOR);
    UUID upload = UUID.randomUUID();

    bulk.confirm(
        caller,
        PROBLEM.toString(),
        new ProblemTestcaseBulkController.ConfirmRequest(upload.toString()));

    verify(uploads)
        .confirm(new BulkTestcaseUpload.Confirm(new AuthoringScope(ACTOR, false), PROBLEM, upload));
    assertThatThrownBy(
            () ->
                bulk.confirm(
                    caller,
                    PROBLEM.toString(),
                    new ProblemTestcaseBulkController.ConfirmRequest("garbage")))
        .isInstanceOf(DomainRuleViolationException.class);
    assertThatThrownBy(
            () ->
                bulk.confirm(
                    caller,
                    PROBLEM.toString(),
                    new ProblemTestcaseBulkController.ConfirmRequest(null)))
        .isInstanceOf(DomainRuleViolationException.class);
    assertThatThrownBy(
            () ->
                bulk.confirm(
                    caller,
                    "zzz",
                    new ProblemTestcaseBulkController.ConfirmRequest(upload.toString())))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void referenceRunWithoutBodyRunsTheWholeSetAndWithTestcaseIdRunsOne() {
    var caller = jwt(ACTOR, "ROLE_ADMIN");
    bulk.run(caller, PROBLEM.toString(), null);
    bulk.run(
        caller,
        PROBLEM.toString(),
        new ProblemTestcaseBulkController.RunRequest(TC.toString(), "PYTHON", "print()"));

    var command = ArgumentCaptor.forClass(ReferenceRunReport.Command.class);
    verify(reference, org.mockito.Mockito.times(2)).run(command.capture());
    assertThat(command.getAllValues().get(0).testcaseId()).isNull();
    assertThat(command.getAllValues().get(0).language()).isNull();
    assertThat(command.getAllValues().get(1).testcaseId()).isEqualTo(TC);
    assertThat(command.getAllValues().get(1).language()).isEqualTo("PYTHON");
    assertThat(command.getAllValues().get(1).scope().admin()).isTrue();
    assertThatThrownBy(
            () ->
                bulk.run(
                    caller,
                    PROBLEM.toString(),
                    new ProblemTestcaseBulkController.RunRequest("bad", null, null)))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Testcase");
  }

  // ---- generation ----

  @Test
  void generateSendsScopeAndProblemWithNoBody() {
    var generation = mock(GenerateTestcasesUseCase.class);
    var controller = new ProblemTestcaseGenerationController(generation);

    controller.generate(jwt(ACTOR, "ROLE_ADMIN"), PROBLEM.toString());

    verify(generation)
        .generate(new TestcaseGeneration.Command(new AuthoringScope(ACTOR, true), PROBLEM));
    assertThatThrownBy(() -> controller.generate(jwt(ACTOR), "x"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  // ---- AI settings and defaults ----

  @Test
  void aiSettingsReadAndUpdateRequireAWholeNumber() {
    var settings = mock(AiGenerationSettingsUseCase.class);
    var controller = new AiGenerationSettingsController(settings);
    when(settings.get()).thenReturn(new TestcaseGeneration.Settings(5));

    assertThat(controller.get().maxPerProblem()).isEqualTo(5);
    controller.update(jwt(ACTOR), JSON.readTree("{\"maxPerProblem\":9}"));
    verify(settings).update(new TestcaseGeneration.UpdateSettings(ACTOR, 9));
    for (String bad :
        new String[] {
          "{}", "{\"maxPerProblem\":\"9\"}", "{\"maxPerProblem\":1.5}", "{\"maxPerProblem\":null}"
        }) {
      assertThatThrownBy(() -> controller.update(jwt(ACTOR), JSON.readTree(bad)))
          .as(bad)
          .isInstanceOf(InvalidRequestException.class);
    }
    assertThatThrownBy(() -> controller.update(jwt(ACTOR), null))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  void authoringDefaultsDelegates() {
    var defaults = mock(ReadAuthoringDefaultsUseCase.class);
    var expected = mock(AuthoringDefaults.class);
    when(defaults.get()).thenReturn(expected);

    assertThat(new AuthoringDefaultsController(defaults).get()).isSameAs(expected);
    org.mockito.Mockito.verify(defaults, org.mockito.Mockito.times(1)).get();
    org.mockito.Mockito.verifyNoMoreInteractions(defaults);
  }
}
