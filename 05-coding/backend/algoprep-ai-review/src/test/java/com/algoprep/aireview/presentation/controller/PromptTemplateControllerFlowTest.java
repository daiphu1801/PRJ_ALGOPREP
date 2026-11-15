package com.algoprep.aireview.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.application.dto.PromptTemplateList;
import com.algoprep.aireview.application.dto.PublishChange;
import com.algoprep.aireview.application.dto.SavePromptDraftCommand;
import com.algoprep.aireview.application.ports.in.GetPromptTemplateHistoryUseCase;
import com.algoprep.aireview.application.ports.in.ListPromptTemplatesUseCase;
import com.algoprep.aireview.application.ports.in.PublishPromptTemplatesUseCase;
import com.algoprep.aireview.application.ports.in.SavePromptDraftUseCase;
import com.algoprep.aireview.domain.exception.AiInvalidRequestException;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.PromptContent;
import com.algoprep.aireview.domain.model.PromptStatus;
import com.algoprep.aireview.domain.model.PromptTemplate;
import com.algoprep.aireview.domain.model.ResponseGuards;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import tools.jackson.databind.json.JsonMapper;

/** Success paths of the prompt-template admin endpoints (DD api F5 sections 14 to 17). */
class PromptTemplateControllerFlowTest {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private final ListPromptTemplatesUseCase list = mock(ListPromptTemplatesUseCase.class);
  private final GetPromptTemplateHistoryUseCase history =
      mock(GetPromptTemplateHistoryUseCase.class);
  private final SavePromptDraftUseCase save = mock(SavePromptDraftUseCase.class);
  private final PublishPromptTemplatesUseCase publish = mock(PublishPromptTemplatesUseCase.class);
  private final PromptTemplateController controller =
      new PromptTemplateController(list, history, save, publish);
  private final UUID admin = UUID.randomUUID();

  private JwtAuthenticationToken auth() {
    Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(admin.toString()).build();
    return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
  }

  private static PromptTemplate template(PromptStatus status) {
    return new PromptTemplate(
        UUID.randomUUID(),
        FeatureCode.MOCK_INTERVIEW,
        "v1.1",
        status,
        new PromptContent("instruction", "m1", new ResponseGuards(true, true, true)),
        "v1",
        status == PromptStatus.ACTIVE ? Instant.EPOCH : null,
        Instant.EPOCH);
  }

  @Test
  void historyMapsEachVersionWithItsInstruction() {
    when(history.history(FeatureCode.MOCK_INTERVIEW))
        .thenReturn(List.of(template(PromptStatus.ACTIVE), template(PromptStatus.ARCHIVED)));

    var result = controller.history("MOCK_INTERVIEW");

    assertThat(result.items()).hasSize(2);
    assertThat(result.items().get(0).status()).isEqualTo("ACTIVE");
    assertThat(result.items().get(0).publishedAt()).isEqualTo(Instant.EPOCH);
    assertThat(result.items().get(1).systemInstruction()).isEqualTo("instruction");
  }

  @Test
  void saveDraftPassesTheActorFromTheTokenAndReturnsTheSavedRow() {
    when(save.save(any())).thenReturn(template(PromptStatus.DRAFT));
    var body =
        JSON.readTree(
            "{\"systemInstruction\":\"new text\",\"modelId\":\"m1\",\"responseGuards\":"
                + "{\"citeLines\":true,\"answerInVietnamese\":true,\"noFullSolution\":true}}");

    var item = controller.saveDraft(auth(), "MOCK_INTERVIEW", body);

    assertThat(item.status()).isEqualTo("DRAFT");
    assertThat(item.responseGuards().noFullSolution()).isTrue();
    var command = ArgumentCaptor.forClass(SavePromptDraftCommand.class);
    verify(save).save(command.capture());
    assertThat(command.getValue().actor()).isEqualTo(admin);
    assertThat(command.getValue().content().systemInstruction()).isEqualTo("new text");
    assertThat(command.getValue().content().modelId()).isEqualTo("m1");
  }

  @Test
  void publishParsesTheChangeListAndReturnsTheLiveRows() {
    UUID expected = UUID.randomUUID();
    when(publish.publish(any(), any()))
        .thenReturn(
            new PromptTemplateList(List.of(template(PromptStatus.ACTIVE)), List.of("m1"), 1));
    var body =
        JSON.readTree(
            "{\"changes\":[{\"featureCode\":\"MOCK_INTERVIEW\",\"expectedActivePromptId\":\""
                + expected
                + "\",\"modelId\":\"m1\"}]}");

    var items = controller.publish(auth(), body);

    assertThat(items.items()).hasSize(1);
    assertThat(items.modelOptions()).containsExactly("m1");
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PublishChange>> changes = ArgumentCaptor.forClass(List.class);
    verify(publish).publish(any(), changes.capture());
    assertThat(changes.getValue())
        .singleElement()
        .satisfies(
            c -> {
              assertThat(c.feature()).isEqualTo(FeatureCode.MOCK_INTERVIEW);
              assertThat(c.expectedActivePromptId()).isEqualTo(expected);
            });
  }

  @Test
  void publishWithAnEmptyChangeListIsRejectedBeforeTheUseCase() {
    assertThatThrownBy(() -> controller.publish(auth(), JSON.readTree("{\"changes\":[]}")))
        .isInstanceOf(AiInvalidRequestException.class);
    verifyNoInteractions(publish);
  }
}
