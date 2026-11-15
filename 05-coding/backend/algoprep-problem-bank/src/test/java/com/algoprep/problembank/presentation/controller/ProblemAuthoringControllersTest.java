package com.algoprep.problembank.presentation.controller;

import static com.algoprep.problembank.presentation.controller.ControllerSupport.jwt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.problembank.application.dto.CreateProblemCommand;
import com.algoprep.problembank.application.dto.ExportProblemsQuery;
import com.algoprep.problembank.application.dto.ExportedCsv;
import com.algoprep.problembank.application.dto.ListProblemsQuery;
import com.algoprep.problembank.application.dto.ProblemAuthoringQuery;
import com.algoprep.problembank.application.dto.ProblemAuthoringView;
import com.algoprep.problembank.application.dto.SaveAiContextCommand;
import com.algoprep.problembank.application.dto.SaveProblemContentCommand;
import com.algoprep.problembank.application.dto.SaveProblemSpecCommand;
import com.algoprep.problembank.application.dto.UpdateProblemStatusCommand;
import com.algoprep.problembank.application.ports.in.BulkProblemActionsUseCase;
import com.algoprep.problembank.application.ports.in.CreateProblemUseCase;
import com.algoprep.problembank.application.ports.in.ExportProblemsCsvUseCase;
import com.algoprep.problembank.application.ports.in.GetAttentionListUseCase;
import com.algoprep.problembank.application.ports.in.GetDuplicateSourceUseCase;
import com.algoprep.problembank.application.ports.in.GetProblemForAuthoringUseCase;
import com.algoprep.problembank.application.ports.in.GetProblemStatsUseCase;
import com.algoprep.problembank.application.ports.in.GetTopicDistributionUseCase;
import com.algoprep.problembank.application.ports.in.ListAuthoringProblemsUseCase;
import com.algoprep.problembank.application.ports.in.SaveAiAuthoringContextUseCase;
import com.algoprep.problembank.application.ports.in.SaveProblemContentUseCase;
import com.algoprep.problembank.application.ports.in.SaveProblemSpecUseCase;
import com.algoprep.problembank.application.ports.in.UpdateProblemPublishStatusUseCase;
import com.algoprep.problembank.domain.exception.ProblemContentInvalidException;
import com.algoprep.problembank.domain.model.AttentionCount;
import com.algoprep.problembank.domain.model.AuthoringListPage;
import com.algoprep.problembank.domain.model.ProblemContentSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Authoring controllers (DD F2 sections 24 to 39): command mapping, 404 for malformed ids. */
class ProblemAuthoringControllersTest {
  private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
  private static final UUID PROBLEM = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final Instant T = Instant.parse("2026-03-01T00:00:00Z");

  private final GetProblemForAuthoringUseCase read = mock(GetProblemForAuthoringUseCase.class);
  private final CreateProblemUseCase create = mock(CreateProblemUseCase.class);
  private final SaveProblemContentUseCase save = mock(SaveProblemContentUseCase.class);
  private final UpdateProblemPublishStatusUseCase status =
      mock(UpdateProblemPublishStatusUseCase.class);
  private final ProblemAuthoringController controller =
      new ProblemAuthoringController(read, create, save, status);

  private static ProblemAuthoringController.ProblemRequest body(
      ProblemAuthoringController.LimitsRequest limits) {
    return new ProblemAuthoringController.ProblemRequest(
        "Two Sum",
        "stmt",
        "cons",
        UUID.randomUUID(),
        limits,
        List.of(UUID.randomUUID()),
        List.of("tag"),
        new ProblemAuthoringController.SolutionRequest("JAVA", "code"),
        List.of(
            new ProblemAuthoringController.ExampleRequest(null, "i1", "o1", null),
            new ProblemAuthoringController.ExampleRequest(PROBLEM, "i2", "o2", "why")),
        null,
        T);
  }

  // ---- ProblemAuthoringController ----

  @Test
  void getFlagsTestcaseReadOnlyForTestcaseManagementAuthority() {
    var view = mock(ProblemAuthoringView.class);
    when(read.get(any())).thenReturn(view);

    assertThat(controller.get(jwt(ACTOR, "PROBLEM_AUTHORING:READ"), PROBLEM.toString()))
        .isSameAs(view);
    assertThat(
            controller.get(
                jwt(ACTOR, "PROBLEM_AUTHORING:READ", "TESTCASE_MANAGEMENT:READ", "ROLE_ADMIN"),
                PROBLEM.toString()))
        .isSameAs(view);

    var query = ArgumentCaptor.forClass(ProblemAuthoringQuery.class);
    verify(read, org.mockito.Mockito.times(2)).get(query.capture());
    assertThat(query.getAllValues().get(0).canReadTestcases()).isFalse();
    assertThat(query.getAllValues().get(0).admin()).isFalse();
    assertThat(query.getAllValues().get(1).canReadTestcases()).isTrue();
    assertThat(query.getAllValues().get(1).admin()).isTrue();
    assertThat(query.getAllValues().get(1).actorId()).isEqualTo(ACTOR);
  }

  @Test
  void aMalformedProblemIdIsTheSame404AsAMissingProblem() {
    var caller = jwt(ACTOR, "PROBLEM_AUTHORING:READ");

    assertThatThrownBy(() -> controller.get(caller, "not-a-uuid"))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Problem not found");
    assertThatThrownBy(
            () ->
                controller.changeStatus(
                    caller, "x", new ProblemAuthoringController.StatusRequest("PUBLISHED", T)))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void createMapsBodyAppliesNoLimitDefaultsAndRereadsTheNewProblem() {
    var snapshot = new ProblemContentSnapshot(PROBLEM, "CODE", ACTOR, "DRAFT", null, T);
    when(create.create(any())).thenReturn(snapshot);

    controller.create(jwt(ACTOR, "PROBLEM_AUTHORING:CREATE"), body(null));

    var command = ArgumentCaptor.forClass(CreateProblemCommand.class);
    verify(create).create(command.capture());
    var c = command.getValue();
    assertThat(c.limits().timeLimitMs()).isNull();
    assertThat(c.content().title()).isEqualTo("Two Sum");
    assertThat(c.content().workedExamples()).extracting("position").containsExactly(1, 2);
    assertThat(c.content().referenceSolution().language()).isEqualTo("JAVA");
    assertThat(c.content().workedExamples().get(1).id()).isEqualTo(PROBLEM);
    var query = ArgumentCaptor.forClass(ProblemAuthoringQuery.class);
    verify(read).get(query.capture());
    assertThat(query.getValue().problemId()).isEqualTo(PROBLEM);
  }

  @Test
  void createWithPartialLimitsAndNoExamplesOrSolution() {
    when(create.create(any()))
        .thenReturn(new ProblemContentSnapshot(PROBLEM, "CODE", ACTOR, "DRAFT", null, T));
    var request =
        new ProblemAuthoringController.ProblemRequest(
            "T",
            null,
            null,
            null,
            new ProblemAuthoringController.LimitsRequest(1000, null, null, 5),
            null,
            null,
            null,
            null,
            PROBLEM,
            null);

    controller.create(jwt(ACTOR, "PROBLEM_AUTHORING:CREATE"), request);

    var command = ArgumentCaptor.forClass(CreateProblemCommand.class);
    verify(create).create(command.capture());
    assertThat(command.getValue().limits().timeLimitMs()).isEqualTo(1000);
    assertThat(command.getValue().limits().maxSubmissionsPerHour()).isEqualTo(5);
    assertThat(command.getValue().duplicatedFromProblemId()).isEqualTo(PROBLEM);
    assertThat(command.getValue().content().referenceSolution()).isNull();
    assertThat(command.getValue().content().workedExamples()).isEmpty();
  }

  @Test
  void createRespondsWithHttp201() throws Exception {
    var method =
        ProblemAuthoringController.class.getDeclaredMethod(
            "create",
            org.springframework.security.oauth2.server.resource.authentication
                .JwtAuthenticationToken.class,
            ProblemAuthoringController.ProblemRequest.class);

    assertThat(method.getAnnotation(ResponseStatus.class).value()).isEqualTo(HttpStatus.CREATED);
  }

  @Test
  void replaceRequiresEachOfTheThreeLimits() {
    var caller = jwt(ACTOR, "PROBLEM_AUTHORING:UPDATE");
    var id = PROBLEM.toString();
    var none = body(null);
    var noTime = body(new ProblemAuthoringController.LimitsRequest(null, 256, 64, null));
    var noMemory = body(new ProblemAuthoringController.LimitsRequest(1000, null, 64, null));
    var noOutput = body(new ProblemAuthoringController.LimitsRequest(1000, 256, null, null));

    assertThatThrownBy(() -> controller.replace(caller, id, none))
        .isInstanceOf(ProblemContentInvalidException.class)
        .satisfies(
            e ->
                assertThat(((ProblemContentInvalidException) e).clientErrorDetails().toString())
                    .contains("limits.timeLimitMs"));
    assertThatThrownBy(() -> controller.replace(caller, id, noTime))
        .isInstanceOf(ProblemContentInvalidException.class)
        .satisfies(
            e ->
                assertThat(((ProblemContentInvalidException) e).clientErrorDetails().toString())
                    .contains("limits.timeLimitMs"));
    assertThatThrownBy(() -> controller.replace(caller, id, noMemory))
        .satisfies(
            e ->
                assertThat(((ProblemContentInvalidException) e).clientErrorDetails().toString())
                    .contains("limits.memoryLimitMb"));
    assertThatThrownBy(() -> controller.replace(caller, id, noOutput))
        .satisfies(
            e ->
                assertThat(((ProblemContentInvalidException) e).clientErrorDetails().toString())
                    .contains("limits.maxOutputSizeKb"));
    org.mockito.Mockito.verifyNoInteractions(save);
  }

  @Test
  void replaceSavesWithTheExpectedVersionAndActionGrantedThenRereads() {
    var limits = new ProblemAuthoringController.LimitsRequest(1000, 256, 64, null);

    controller.replace(
        jwt(ACTOR, "PROBLEM_AUTHORING:UPDATE", "ROLE_ADMIN"), PROBLEM.toString(), body(limits));

    var command = ArgumentCaptor.forClass(SaveProblemContentCommand.class);
    verify(save).save(command.capture());
    assertThat(command.getValue().expectedUpdatedAt()).isEqualTo(T);
    assertThat(command.getValue().admin()).isTrue();
    assertThat(command.getValue().actionGranted()).isTrue();
    assertThat(command.getValue().content().limits().memoryLimitMb()).isEqualTo(256);
    verify(read).get(any());
  }

  @Test
  void changeStatusPassesTargetStatusAndExpectedVersion() {
    controller.changeStatus(
        jwt(ACTOR, "PROBLEM_AUTHORING:UPDATE"),
        PROBLEM.toString(),
        new ProblemAuthoringController.StatusRequest("PUBLISHED", T));

    var command = ArgumentCaptor.forClass(UpdateProblemStatusCommand.class);
    verify(status).update(command.capture());
    assertThat(command.getValue().status()).isEqualTo("PUBLISHED");
    assertThat(command.getValue().expectedUpdatedAt()).isEqualTo(T);
    assertThat(command.getValue().actorId()).isEqualTo(ACTOR);
  }

  // ---- helpers controller ----

  @Test
  void helpersMapDuplicateSourceAiContextAndStats() {
    var duplicate = mock(GetDuplicateSourceUseCase.class);
    var ai = mock(SaveAiAuthoringContextUseCase.class);
    var stats = mock(GetProblemStatsUseCase.class);
    var helpers = new ProblemAuthoringHelpersController(duplicate, ai, stats);
    var caller = jwt(ACTOR, "ROLE_ADMIN");

    helpers.duplicateSource(caller, PROBLEM.toString());
    helpers.stats(caller, PROBLEM.toString());
    helpers.saveAiContext(
        caller,
        PROBLEM.toString(),
        new ProblemAuthoringHelpersController.AiContextRequest("brief", true, false, T));

    var query = ArgumentCaptor.forClass(ProblemAuthoringQuery.class);
    verify(duplicate).get(query.capture());
    assertThat(query.getValue().admin()).isTrue();
    assertThat(query.getValue().canReadTestcases()).isFalse();
    verify(stats).get(any());
    var command = ArgumentCaptor.forClass(SaveAiContextCommand.class);
    verify(ai).save(command.capture());
    assertThat(command.getValue().briefText()).isEqualTo("brief");
    assertThat(command.getValue().noFullCode()).isTrue();
    assertThat(command.getValue().questionOnly()).isFalse();
    assertThat(command.getValue().expectedUpdatedAt()).isEqualTo(T);
    assertThatThrownBy(() -> helpers.stats(caller, "zzz"))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> helpers.duplicateSource(caller, "zzz"))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(
            () ->
                helpers.saveAiContext(
                    caller,
                    "zzz",
                    new ProblemAuthoringHelpersController.AiContextRequest(null, null, null, null)))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  // ---- list controller ----

  @Test
  void listWrapsPageMetaAndScopeTotals() {
    var list = mock(ListAuthoringProblemsUseCase.class);
    var distribution = mock(GetTopicDistributionUseCase.class);
    var attention = mock(GetAttentionListUseCase.class);
    var controller = new ProblemAuthoringListController(list, distribution, attention);
    when(list.list(any())).thenReturn(new AuthoringListPage(List.of(), 2, 10, 15, 2, 40, 12));
    when(attention.get(any())).thenReturn(List.of(new AttentionCount("LOW_AC", 3)));

    var response =
        controller.list(
            jwt(ACTOR, "PROBLEM_AUTHORING:READ"),
            new ProblemAuthoringListController.ListParams("q", "L", "DRAFT", "code", "asc", 2, 10));

    assertThat(response.page().number()).isEqualTo(2);
    assertThat(response.page().totalItems()).isEqualTo(15);
    assertThat(response.page().totalPages()).isEqualTo(2);
    assertThat(response.totals().total()).isEqualTo(40);
    assertThat(response.totals().published()).isEqualTo(12);
    var query = ArgumentCaptor.forClass(ListProblemsQuery.class);
    verify(list).list(query.capture());
    assertThat(query.getValue().sort()).isEqualTo("code");
    assertThat(query.getValue().pageSize()).isEqualTo(10);
    assertThat(controller.attention(jwt(ACTOR)).items()).hasSize(1);
    controller.topicDistribution(jwt(ACTOR, "ROLE_ADMIN"));
    verify(distribution)
        .get(new com.algoprep.problembank.application.dto.AuthoringScope(ACTOR, true));
  }

  // ---- bulk controller ----

  @Test
  void bulkActionsForwardScopeIdsAndTheirOwnParameter() {
    var bulk = mock(BulkProblemActionsUseCase.class);
    var controller = new ProblemAuthoringBulkController(bulk, mock(ExportProblemsCsvUseCase.class));
    var caller = jwt(ACTOR, "ROLE_ADMIN");
    UUID level = UUID.randomUUID();
    UUID topic = UUID.randomUUID();
    var body = new ProblemAuthoringBulkController.BulkRequest(List.of(PROBLEM), level, topic);
    var scope = new com.algoprep.problembank.application.dto.AuthoringScope(ACTOR, true);

    controller.publish(caller, body);
    controller.unpublish(caller, body);
    controller.difficulty(caller, body);
    controller.topic(caller, body);
    controller.delete(caller, body);

    verify(bulk).publish(scope, List.of(PROBLEM));
    verify(bulk).unpublish(scope, List.of(PROBLEM));
    verify(bulk).changeDifficulty(scope, List.of(PROBLEM), level);
    verify(bulk).assignTopic(scope, List.of(PROBLEM), topic);
    verify(bulk).delete(scope, List.of(PROBLEM));
  }

  @Test
  void exportReturnsCsvAttachmentWithTheServerFilename() {
    var export = mock(ExportProblemsCsvUseCase.class);
    var controller =
        new ProblemAuthoringBulkController(mock(BulkProblemActionsUseCase.class), export);
    when(export.export(any())).thenReturn(new ExportedCsv("problems.csv", "a,b".getBytes()));

    var response =
        controller.export(
            jwt(ACTOR, "PROBLEM_AUTHORING:READ"),
            new ProblemAuthoringBulkController.ExportRequest(
                null, new ProblemAuthoringBulkController.FilterRequest("q", "L", "DRAFT")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType().getSubtype()).isEqualTo("csv");
    assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
        .contains("attachment")
        .contains("problems.csv");
    assertThat(response.getBody()).isEqualTo("a,b".getBytes());
    var query = ArgumentCaptor.forClass(ExportProblemsQuery.class);
    verify(export).export(query.capture());
    assertThat(query.getValue().filter().status()).isEqualTo("DRAFT");
  }

  @Test
  void exportByIdsHasNoFilter() {
    var export = mock(ExportProblemsCsvUseCase.class);
    var controller =
        new ProblemAuthoringBulkController(mock(BulkProblemActionsUseCase.class), export);
    when(export.export(any())).thenReturn(new ExportedCsv("p.csv", new byte[0]));

    controller.export(
        jwt(ACTOR, "PROBLEM_AUTHORING:READ"),
        new ProblemAuthoringBulkController.ExportRequest(List.of(PROBLEM), null));

    var query = ArgumentCaptor.forClass(ExportProblemsQuery.class);
    verify(export).export(query.capture());
    assertThat(query.getValue().filter()).isNull();
    assertThat(query.getValue().problemIds()).containsExactly(PROBLEM);
  }

  // ---- spec controller ----

  private final SaveProblemSpecUseCase specs = mock(SaveProblemSpecUseCase.class);
  private final ProblemSpecController spec = new ProblemSpecController(specs);

  @Test
  void specSaveParsesExpectedVersionAndKeepsDecimalPrecision() {
    spec.save(
        jwt(ACTOR, "PROBLEM_AUTHORING:UPDATE"),
        PROBLEM.toString(),
        "{\"expectedUpdatedAt\":\"2026-03-01T00:00:00Z\",\"epsilonValue\":0.1000000000000000055511151231257827}");

    var command = ArgumentCaptor.forClass(SaveProblemSpecCommand.class);
    verify(specs).save(command.capture());
    assertThat(command.getValue().expectedUpdatedAt()).isEqualTo(T);
    assertThat(command.getValue().body().path("epsilonValue").decimalValue().toPlainString())
        .isEqualTo("0.1000000000000000055511151231257827");
    assertThat(command.getValue().admin()).isFalse();
  }

  @Test
  void specSaveRejectsMalformedBodiesIdsAndTimestampsWith400Prb001() {
    var caller = jwt(ACTOR, "PROBLEM_AUTHORING:UPDATE");
    String id = PROBLEM.toString();
    String[] bodies = {
      "not json", "[]", "{}", "{\"expectedUpdatedAt\":5}", "{\"expectedUpdatedAt\":\"yesterday\"}"
    };
    for (String bad : bodies) {
      assertThatThrownBy(() -> spec.save(caller, id, bad))
          .as(bad)
          .isInstanceOf(InvalidRequestException.class);
    }
    assertThatThrownBy(
            () -> spec.save(caller, "nope", "{\"expectedUpdatedAt\":\"2026-03-01T00:00:00Z\"}"))
        .isInstanceOf(InvalidRequestException.class);
    org.mockito.Mockito.verifyNoInteractions(specs);
  }
}
