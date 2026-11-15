package com.algoprep.problembank.presentation.controller;

import static com.algoprep.problembank.presentation.controller.ControllerSupport.jwt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.exception.PermissionDeniedException;
import com.algoprep.problembank.application.dto.CatalogCommand;
import com.algoprep.problembank.application.dto.CatalogQuery;
import com.algoprep.problembank.application.ports.in.CreateProblemLevelUseCase;
import com.algoprep.problembank.application.ports.in.CreateProblemTopicUseCase;
import com.algoprep.problembank.application.ports.in.DeleteProblemLevelUseCase;
import com.algoprep.problembank.application.ports.in.DeleteProblemTopicUseCase;
import com.algoprep.problembank.application.ports.in.ListProblemLevelsUseCase;
import com.algoprep.problembank.application.ports.in.ListProblemTopicsUseCase;
import com.algoprep.problembank.application.ports.in.ReorderProblemLevelsUseCase;
import com.algoprep.problembank.application.ports.in.ReorderProblemTopicsUseCase;
import com.algoprep.problembank.application.ports.in.UpdateProblemLevelUseCase;
import com.algoprep.problembank.application.ports.in.UpdateProblemTopicUseCase;
import com.algoprep.problembank.domain.model.CatalogItem;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Level and topic catalogue endpoints (DD F2 sections 1 to 8): admin-only writes, PRB codes. */
class CatalogControllersTest {
  private static final UUID ADMIN = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
  private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final CatalogItem ITEM = new CatalogItem(ID, "easy", "Easy", 1, 4L);

  private final ListProblemLevelsUseCase listLevels = mock(ListProblemLevelsUseCase.class);
  private final CreateProblemLevelUseCase createLevel = mock(CreateProblemLevelUseCase.class);
  private final UpdateProblemLevelUseCase updateLevel = mock(UpdateProblemLevelUseCase.class);
  private final ReorderProblemLevelsUseCase reorderLevels = mock(ReorderProblemLevelsUseCase.class);
  private final DeleteProblemLevelUseCase deleteLevel = mock(DeleteProblemLevelUseCase.class);
  private final ProblemLevelsController levels =
      new ProblemLevelsController(listLevels, createLevel, updateLevel, reorderLevels, deleteLevel);

  private final ListProblemTopicsUseCase listTopics = mock(ListProblemTopicsUseCase.class);
  private final CreateProblemTopicUseCase createTopic = mock(CreateProblemTopicUseCase.class);
  private final UpdateProblemTopicUseCase updateTopic = mock(UpdateProblemTopicUseCase.class);
  private final ReorderProblemTopicsUseCase reorderTopics = mock(ReorderProblemTopicsUseCase.class);
  private final DeleteProblemTopicUseCase deleteTopic = mock(DeleteProblemTopicUseCase.class);
  private final ProblemTopicsController topics =
      new ProblemTopicsController(listTopics, createTopic, updateTopic, reorderTopics, deleteTopic);

  private static JsonNode json(String text) {
    return JSON.readTree(text);
  }

  // ---- levels ----

  @Test
  void levelListMapsCatalogueItemsAndPassesTheCountsFlagAndAdminRole() {
    when(listLevels.list(any())).thenReturn(List.of(ITEM));

    var result = levels.list(jwt(ADMIN, "ROLE_ADMIN"), true);

    assertThat(result.items())
        .singleElement()
        .satisfies(
            i -> {
              assertThat(i.displayName()).isEqualTo("Easy");
              assertThat(i.problemCount()).isEqualTo(4L);
            });
    var query = ArgumentCaptor.forClass(CatalogQuery.class);
    verify(listLevels).list(query.capture());
    assertThat(query.getValue().admin()).isTrue();
    assertThat(query.getValue().includeCounts()).isTrue();
    levels.list(jwt(ADMIN), false);
    verify(listLevels, org.mockito.Mockito.times(2)).list(query.capture());
    assertThat(query.getValue().admin()).isFalse();
  }

  @Test
  void levelCreateReadsDisplayNameAndReturnsTheFirstItem() throws Exception {
    when(createLevel.execute(any())).thenReturn(List.of(ITEM));

    var created = levels.create(jwt(ADMIN, "ROLE_ADMIN"), json("{\"displayName\":\"Easy\"}"));

    assertThat(created.code()).isEqualTo("easy");
    var command = ArgumentCaptor.forClass(CatalogCommand.class);
    verify(createLevel).execute(command.capture());
    assertThat(command.getValue().name()).isEqualTo("Easy");
    assertThat(command.getValue().id()).isNull();
    assertThat(command.getValue().actorId()).isEqualTo(ADMIN);
    assertThat(
            ProblemLevelsController.class
                .getDeclaredMethod(
                    "create",
                    org.springframework.security.oauth2.server.resource.authentication
                        .JwtAuthenticationToken.class,
                    JsonNode.class)
                .getAnnotation(ResponseStatus.class)
                .value())
        .isEqualTo(HttpStatus.CREATED);
  }

  @Test
  void levelWritesAreRefusedForNonAdminsBeforeTheBodyIsParsed() {
    var instructor = jwt(ADMIN, "PROBLEM_AUTHORING:CREATE");

    assertThatThrownBy(() -> levels.create(instructor, json("{\"displayName\":\"X\"}")))
        .isInstanceOf(PermissionDeniedException.class);
    assertThatThrownBy(() -> levels.update(instructor, ID.toString(), null))
        .isInstanceOf(PermissionDeniedException.class);
    assertThatThrownBy(() -> levels.reorder(instructor, null))
        .isInstanceOf(PermissionDeniedException.class);
    assertThatThrownBy(() -> levels.delete(instructor, ID.toString()))
        .isInstanceOf(PermissionDeniedException.class);
    org.mockito.Mockito.verifyNoInteractions(createLevel, updateLevel, reorderLevels, deleteLevel);
  }

  @Test
  void levelUpdateRejectsMalformedIdAndTakesNullNameForNonStringField() {
    var admin = jwt(ADMIN, "ROLE_ADMIN");
    when(updateLevel.execute(any())).thenReturn(List.of(ITEM));

    assertThatThrownBy(() -> levels.update(admin, "bad-id", json("{}")))
        .isInstanceOf(InvalidRequestException.class);
    levels.update(admin, ID.toString(), json("{\"displayName\":7}"));
    var command = ArgumentCaptor.forClass(CatalogCommand.class);
    verify(updateLevel).execute(command.capture());
    assertThat(command.getValue().name()).isNull();
    assertThat(command.getValue().id()).isEqualTo(ID);
  }

  @Test
  void levelReorderParsesOrderedIdsAndRejectsEverythingElseWithPrb049() {
    var admin = jwt(ADMIN, "ROLE_ADMIN");
    when(reorderLevels.execute(any())).thenReturn(List.of(ITEM));

    var result = levels.reorder(admin, json("{\"orderedIds\":[\"" + ID + "\"]}"));

    assertThat(result.items()).hasSize(1);
    var command = ArgumentCaptor.forClass(CatalogCommand.class);
    verify(reorderLevels).execute(command.capture());
    assertThat(command.getValue().orderedIds()).containsExactly(ID);
    for (String bad :
        new String[] {
          "{}", "{\"orderedIds\":\"x\"}", "{\"orderedIds\":[1]}", "{\"orderedIds\":[\"nope\"]}"
        }) {
      assertThatThrownBy(() -> levels.reorder(admin, json(bad)))
          .as(bad)
          .isInstanceOf(DomainRuleViolationException.class);
    }
    assertThatThrownBy(() -> levels.reorder(admin, null))
        .isInstanceOf(DomainRuleViolationException.class);
  }

  @Test
  void levelDeleteForwardsTheIdAndRespondsNoContent() throws Exception {
    levels.delete(jwt(ADMIN, "ROLE_ADMIN"), ID.toString());

    var command = ArgumentCaptor.forClass(CatalogCommand.class);
    verify(deleteLevel).execute(command.capture());
    assertThat(command.getValue().id()).isEqualTo(ID);
    assertThat(
            ProblemLevelsController.class
                .getDeclaredMethod(
                    "delete",
                    org.springframework.security.oauth2.server.resource.authentication
                        .JwtAuthenticationToken.class,
                    String.class)
                .getAnnotation(ResponseStatus.class)
                .value())
        .isEqualTo(HttpStatus.NO_CONTENT);
  }

  // ---- topics ----

  @Test
  void topicEndpointsUseTheNameFieldInsteadOfDisplayName() {
    var admin = jwt(ADMIN, "ROLE_ADMIN");
    when(createTopic.execute(any())).thenReturn(List.of(ITEM));
    when(updateTopic.execute(any())).thenReturn(List.of(ITEM));
    when(reorderTopics.execute(any())).thenReturn(List.of(ITEM));
    when(listTopics.list(any())).thenReturn(List.of(ITEM));

    assertThat(topics.create(admin, json("{\"name\":\"Graph\"}")).name()).isEqualTo("Easy");
    topics.update(admin, ID.toString(), json("{\"name\":\"Trees\"}"));
    topics.reorder(admin, json("{\"orderedIds\":[\"" + ID + "\"]}"));
    topics.delete(admin, ID.toString());
    assertThat(topics.list(admin, false).items()).hasSize(1);

    var command = ArgumentCaptor.forClass(CatalogCommand.class);
    verify(createTopic).execute(command.capture());
    assertThat(command.getValue().name()).isEqualTo("Graph");
    verify(updateTopic).execute(command.capture());
    assertThat(command.getValue().name()).isEqualTo("Trees");
    verify(deleteTopic).execute(any());
  }

  @Test
  void topicWritesAreAdminOnly() {
    var instructor = jwt(ADMIN, "PROBLEM_AUTHORING:UPDATE");

    assertThatThrownBy(() -> topics.create(instructor, json("{\"name\":\"X\"}")))
        .isInstanceOf(PermissionDeniedException.class);
    assertThatThrownBy(() -> topics.update(instructor, ID.toString(), json("{}")))
        .isInstanceOf(PermissionDeniedException.class);
    assertThatThrownBy(() -> topics.reorder(instructor, json("{}")))
        .isInstanceOf(PermissionDeniedException.class);
    assertThatThrownBy(() -> topics.delete(instructor, ID.toString()))
        .isInstanceOf(PermissionDeniedException.class);
  }
}
