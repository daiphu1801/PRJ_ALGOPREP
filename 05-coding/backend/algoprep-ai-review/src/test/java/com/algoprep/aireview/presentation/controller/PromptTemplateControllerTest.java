package com.algoprep.aireview.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.application.dto.PromptTemplateList;
import com.algoprep.aireview.application.ports.in.GetPromptTemplateHistoryUseCase;
import com.algoprep.aireview.application.ports.in.ListPromptTemplatesUseCase;
import com.algoprep.aireview.application.ports.in.PublishPromptTemplatesUseCase;
import com.algoprep.aireview.application.ports.in.SavePromptDraftUseCase;
import com.algoprep.aireview.domain.exception.AiPromptRuleException;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.PromptContent;
import com.algoprep.aireview.domain.model.PromptStatus;
import com.algoprep.aireview.domain.model.PromptTemplate;
import com.algoprep.aireview.domain.model.ResponseGuards;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import tools.jackson.databind.json.JsonMapper;

class PromptTemplateControllerTest {
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

  private static PromptTemplate row(ResponseGuards guards) {
    return new PromptTemplate(
        UUID.randomUUID(),
        FeatureCode.SOLUTION_REVIEW,
        "v1.0",
        PromptStatus.DRAFT,
        new PromptContent("text", "m1", guards),
        "v1",
        null,
        Instant.EPOCH);
  }

  private static String preAuthorize(String method, Class<?>... types) throws Exception {
    Method m = PromptTemplateController.class.getDeclaredMethod(method, types);
    return m.getAnnotation(PreAuthorize.class).value();
  }

  @Test
  void everyEndpointDeclaresBothLayersAndTheRightAction() throws Exception {
    assertThat(preAuthorize("list"))
        .isEqualTo("hasAuthority('ROLE_ADMIN') and hasAuthority('AI_CONFIG:READ')");
    assertThat(preAuthorize("history", String.class)).contains("ROLE_ADMIN", "AI_CONFIG:READ");
    assertThat(
            preAuthorize(
                "saveDraft",
                JwtAuthenticationToken.class,
                String.class,
                tools.jackson.databind.JsonNode.class))
        .contains("ROLE_ADMIN", "AI_CONFIG:UPDATE");
    assertThat(
            preAuthorize(
                "publish", JwtAuthenticationToken.class, tools.jackson.databind.JsonNode.class))
        .isEqualTo("hasAuthority('ROLE_ADMIN') and hasAuthority('AI_CONFIG:UPDATE')");
  }

  @Test
  void listMapsRowsGuardsAndTheReadOnlyTokenCap() {
    when(list.list())
        .thenReturn(
            new PromptTemplateList(
                List.of(row(new ResponseGuards(true, false, true)), row(null)),
                List.of("m1"),
                4096));
    var items = controller.list();
    assertThat(items.modelOptions()).containsExactly("m1");
    assertThat(items.items().get(0).responseGuards().noFullSolution()).isTrue();
    assertThat(items.items().get(0).maxTokens()).isEqualTo(4096);
    assertThat(items.items().get(1).responseGuards()).isNull();
  }

  @Test
  void historyRejectsAnUnknownFeatureBeforeTheUseCase() {
    assertThatThrownBy(() -> controller.history("OTHER"))
        .isInstanceOf(com.algoprep.aireview.domain.exception.AiInvalidRequestException.class);
  }

  @Test
  void aMissingModelIsPassedOnAsAnEmptyIdSoTheUseCaseAnswersAir202() {
    when(save.save(any())).thenThrow(new AiPromptRuleException("AIR-202", "no"));
    var body =
        JSON.readTree(
            "{\"systemInstruction\":\"x\",\"responseGuards\":{\"citeLines\":true,"
                + "\"answerInVietnamese\":true,\"noFullSolution\":true}}");
    assertThatThrownBy(() -> controller.saveDraft(auth(), "SOLUTION_REVIEW", body))
        .isInstanceOf(AiPromptRuleException.class);
  }

  @Test
  void adviceShapesForbiddenAndUnreadableBodies() {
    var advice = new PromptTemplateErrorAdvice();
    var denied = advice.denied();
    assertThat(denied.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(denied.getProperties()).containsEntry("errorCode", "AIR-301");
  }
}
