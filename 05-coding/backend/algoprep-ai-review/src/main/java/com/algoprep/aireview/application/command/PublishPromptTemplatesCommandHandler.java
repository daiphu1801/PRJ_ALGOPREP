package com.algoprep.aireview.application.command;

import com.algoprep.aireview.application.dto.PromptTemplateList;
import com.algoprep.aireview.application.dto.PublishChange;
import com.algoprep.aireview.application.ports.in.PublishPromptTemplatesUseCase;
import com.algoprep.aireview.application.ports.out.AiAuditPort;
import com.algoprep.aireview.application.ports.out.AiModelOptionsPort;
import com.algoprep.aireview.domain.exception.AiPromptConflictException;
import com.algoprep.aireview.domain.exception.AiPromptRuleException;
import com.algoprep.aireview.domain.model.AiAuditEntry;
import com.algoprep.aireview.domain.model.CallOptions;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.PromptContent;
import com.algoprep.aireview.domain.model.PromptStatus;
import com.algoprep.aireview.domain.model.PromptTemplate;
import com.algoprep.aireview.domain.ports.out.PromptTemplateRepository;
import com.algoprep.common.exception.ServiceUnavailableException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Endpoint 17 (BR-AI-13 publish steps), one transaction: every feature is checked first, then every
 * changed feature is archived and replaced, one audit row each. Any failure, including the audit
 * write (AIR-503), rolls everything back. The new ACTIVE row has a different id from the old one,
 * and the Solution Review cache key contains that id, so old cached reviews stop matching (AC-04).
 */
@Service
@Transactional
class PublishPromptTemplatesCommandHandler implements PublishPromptTemplatesUseCase {
  static final String ACTION = "AI_PROMPT_PUBLISHED";
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private record Plan(
      PublishChange change, PromptTemplate active, PromptTemplate draft, PromptContent content) {}

  private final PromptTemplateRepository templates;
  private final AiModelOptionsPort models;
  private final AiAuditPort audit;

  PublishPromptTemplatesCommandHandler(
      PromptTemplateRepository templates, AiModelOptionsPort models, AiAuditPort audit) {
    this.templates = templates;
    this.models = models;
    this.audit = audit;
  }

  @Override
  public PromptTemplateList publish(UUID actor, List<PublishChange> changes) {
    List<PublishChange> ordered =
        changes.stream().sorted(Comparator.comparing(PublishChange::feature)).toList();
    List<Plan> plans = new ArrayList<>();
    for (PublishChange change : ordered) {
      plans.add(plan(change));
    }
    List<Plan> real =
        plans.stream().filter(p -> !p.content().equals(p.active().content())).toList();
    if (real.isEmpty()) {
      throw new AiPromptRuleException(
          AiPromptRuleException.NOTHING_CHANGED, "Nothing differs from the active prompt");
    }
    for (Plan plan : real) {
      apply(actor, plan);
    }
    return new PromptTemplateList(
        templates.listLive().stream().filter(t -> t.status() == PromptStatus.ACTIVE).toList(),
        models.allowed(),
        CallOptions.DEFAULT_MAX_TOKENS);
  }

  private Plan plan(PublishChange change) {
    FeatureCode feature = change.feature();
    templates.lock(feature);
    PromptTemplate active =
        templates.find(feature, PromptStatus.ACTIVE).orElseThrow(AiPromptConflictException::new);
    if (!active.id().equals(change.expectedActivePromptId())) {
      throw new AiPromptConflictException();
    }
    PromptTemplate draft = templates.find(feature, PromptStatus.DRAFT).orElse(null);
    PromptContent base = draft != null ? draft.content() : active.content();
    PromptContent content =
        new PromptContent(
            pick(change.systemInstruction(), base.systemInstruction()),
            pick(change.modelId(), base.modelId()),
            change.guards() != null ? change.guards() : base.guards());
    // An inherited ACTIVE model is trusted even when the allowed list changed since (the seed uses
    // the alias 'default'); anything else must be on the list.
    if (!content.modelId().equals(active.content().modelId())
        && !models.allowed().contains(content.modelId())) {
      throw new AiPromptRuleException(
          AiPromptRuleException.MODEL_NOT_ALLOWED, "Model is not on the allowed list");
    }
    return new Plan(change, active, draft, content);
  }

  private void apply(UUID actor, Plan plan) {
    FeatureCode feature = plan.change().feature();
    templates.archive(plan.active().id());
    PromptTemplate created =
        plan.draft() != null
            ? templates.promote(plan.draft().id(), plan.content())
            : templates.createActive(feature, plan.content(), actor);
    try {
      audit.record(
          new AiAuditEntry(
              actor,
              ACTION,
              "PROMPT_TEMPLATE",
              feature.name(),
              snapshot(plan.active()),
              snapshot(created)));
    } catch (RuntimeException failure) {
      throw new ServiceUnavailableException(
          "AIR-503", "Audit write unavailable; change rolled back", failure);
    }
  }

  /** The instruction text is deliberately left out: it can be long and is configuration data. */
  private static String snapshot(PromptTemplate template) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("version", template.version());
    map.put("modelId", template.content().modelId());
    map.put("responseGuards", template.content().guards());
    return JSON.writeValueAsString(map);
  }

  private static String pick(String sent, String fallback) {
    return sent != null ? sent : fallback;
  }
}
