package com.algoprep.aireview.application.command.prompt;

import com.algoprep.aireview.application.dto.prompt.SavePromptDraftCommand;
import com.algoprep.aireview.application.ports.in.prompt.SavePromptDraftUseCase;
import com.algoprep.aireview.application.ports.out.AiModelOptionsPort;
import com.algoprep.aireview.domain.exception.prompt.AiPromptRuleException;
import com.algoprep.aireview.domain.model.prompt.PromptContent;
import com.algoprep.aireview.domain.model.prompt.PromptStatus;
import com.algoprep.aireview.domain.model.prompt.PromptTemplate;
import com.algoprep.aireview.domain.ports.out.PromptTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Endpoint 16 (BR-AI-13 save steps): update the single DRAFT in place or create it with the next
 * version. Writes no audit row (nothing reaches users until publish, DD api F5 section 16.1).
 */
@Service
@Transactional
class SavePromptDraftCommandHandler implements SavePromptDraftUseCase {
  private final PromptTemplateRepository templates;
  private final AiModelOptionsPort models;

  SavePromptDraftCommandHandler(PromptTemplateRepository templates, AiModelOptionsPort models) {
    this.templates = templates;
    this.models = models;
  }

  @Override
  public PromptTemplate save(SavePromptDraftCommand command) {
    PromptContent requested = command.content();
    if (!models.allowed().contains(requested.modelId())) {
      throw new AiPromptRuleException(
          AiPromptRuleException.MODEL_NOT_ALLOWED, "Model is not on the allowed list");
    }
    templates.lock(command.feature());
    var existing = templates.find(command.feature(), PromptStatus.DRAFT);
    PromptContent content = requested;
    if (requested.maxTokens() == null) {
      var previous = existing.or(() -> templates.find(command.feature(), PromptStatus.ACTIVE));
      content =
          new PromptContent(
              requested.systemInstruction(),
              requested.modelId(),
              requested.guards(),
              previous.map(t -> t.content().maxTokens()).orElse(null));
    }
    content.validateMaxTokens(command.feature(), models.maxOutputTokens(content.modelId()));
    PromptContent validated = content;
    return existing
        .map(draft -> templates.updateDraft(draft.id(), validated))
        .orElseGet(() -> templates.createDraft(command.feature(), validated, command.actor()));
  }
}
