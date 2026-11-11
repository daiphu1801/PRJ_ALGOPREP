package com.algoprep.aireview.presentation.controller;

import com.algoprep.aireview.application.dto.PromptTemplateList;
import com.algoprep.aireview.application.dto.SavePromptDraftCommand;
import com.algoprep.aireview.application.ports.in.GetPromptTemplateHistoryUseCase;
import com.algoprep.aireview.application.ports.in.ListPromptTemplatesUseCase;
import com.algoprep.aireview.application.ports.in.PublishPromptTemplatesUseCase;
import com.algoprep.aireview.application.ports.in.SavePromptDraftUseCase;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.PromptContent;
import com.algoprep.aireview.domain.model.PromptTemplate;
import com.algoprep.aireview.domain.model.ResponseGuards;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/**
 * DD api F5 sections 14 to 17 (ADM0301). Both layers are checked here: the matrix cell and the
 * ADMIN base category; {@code AI_TOKEN_BUDGET} never substitutes for {@code AI_CONFIG}.
 */
@RestController
@RequestMapping("/api/v1/ai/admin/prompt-templates")
class PromptTemplateController {
  private static final String READ =
      "hasAuthority('ROLE_ADMIN') and hasAuthority('AI_CONFIG:READ')";
  private static final String UPDATE =
      "hasAuthority('ROLE_ADMIN') and hasAuthority('AI_CONFIG:UPDATE')";

  private final ListPromptTemplatesUseCase list;
  private final GetPromptTemplateHistoryUseCase history;
  private final SavePromptDraftUseCase save;
  private final PublishPromptTemplatesUseCase publish;

  PromptTemplateController(
      ListPromptTemplatesUseCase list,
      GetPromptTemplateHistoryUseCase history,
      SavePromptDraftUseCase save,
      PublishPromptTemplatesUseCase publish) {
    this.list = list;
    this.history = history;
    this.save = save;
    this.publish = publish;
  }

  record Guards(boolean citeLines, boolean answerInVietnamese, boolean noFullSolution) {}

  record Item(
      UUID id,
      FeatureCode featureCode,
      String version,
      String status,
      String systemInstruction,
      String modelId,
      Guards responseGuards,
      String responseSchemaVersion,
      int maxTokens,
      Instant publishedAt,
      Instant createdAt) {}

  record Items(List<Item> items, List<String> modelOptions) {}

  record HistoryItem(
      UUID id,
      String version,
      String status,
      Instant publishedAt,
      Instant createdAt,
      String systemInstruction) {}

  record History(List<HistoryItem> items) {}

  private static Item item(PromptTemplate t, int maxTokens) {
    ResponseGuards g = t.content().guards();
    return new Item(
        t.id(),
        t.featureCode(),
        t.version(),
        t.status().name(),
        t.content().systemInstruction(),
        t.content().modelId(),
        g == null ? null : new Guards(g.citeLines(), g.answerInVietnamese(), g.noFullSolution()),
        t.responseSchemaVersion(),
        maxTokens,
        t.publishedAt(),
        t.createdAt());
  }

  private static Items items(PromptTemplateList result) {
    return new Items(
        result.items().stream().map(t -> item(t, result.maxTokens())).toList(),
        result.modelOptions());
  }

  @GetMapping
  @PreAuthorize(READ)
  Items list() {
    return items(list.list());
  }

  @GetMapping("/{featureCode}/history")
  @PreAuthorize(READ)
  History history(@PathVariable String featureCode) {
    return new History(
        history.history(PromptRequests.feature(featureCode)).stream()
            .map(
                t ->
                    new HistoryItem(
                        t.id(),
                        t.version(),
                        t.status().name(),
                        t.publishedAt(),
                        t.createdAt(),
                        t.content().systemInstruction()))
            .toList());
  }

  @PutMapping("/{featureCode}/draft")
  @PreAuthorize(UPDATE)
  Item saveDraft(
      JwtAuthenticationToken caller, @PathVariable String featureCode, @RequestBody JsonNode body) {
    FeatureCode feature = PromptRequests.feature(featureCode);
    String instruction = PromptRequests.instruction(body);
    ResponseGuards guards = PromptRequests.guards(feature, body.get("responseGuards"), false);
    String model = PromptRequests.modelId(body.get("modelId"));
    // A missing or non-text model can never be on the allowed list, so it fails as AIR-202.
    PromptContent content = new PromptContent(instruction, model == null ? "" : model, guards);
    PromptTemplate saved =
        save.save(
            new SavePromptDraftCommand(
                UUID.fromString(caller.getToken().getSubject()), feature, content));
    return item(saved, com.algoprep.aireview.domain.model.CallOptions.DEFAULT_MAX_TOKENS);
  }

  @PostMapping("/publish")
  @PreAuthorize(UPDATE)
  Items publish(JwtAuthenticationToken caller, @RequestBody JsonNode body) {
    var changes = PromptRequests.changes(body);
    return items(publish.publish(UUID.fromString(caller.getToken().getSubject()), changes));
  }
}
