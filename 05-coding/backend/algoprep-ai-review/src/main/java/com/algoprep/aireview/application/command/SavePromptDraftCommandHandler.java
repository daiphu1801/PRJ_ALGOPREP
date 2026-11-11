package com.algoprep.aireview.application.command;

import com.algoprep.aireview.application.dto.SavePromptDraftCommand;
import com.algoprep.aireview.application.ports.in.SavePromptDraftUseCase;
import com.algoprep.aireview.application.ports.out.AiModelOptionsPort;
import com.algoprep.aireview.domain.exception.AiPromptRuleException;
import com.algoprep.aireview.domain.model.PromptContent;
import com.algoprep.aireview.domain.model.PromptStatus;
import com.algoprep.aireview.domain.model.PromptTemplate;
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
    PromptContent content = command.content();
    if (!models.allowed().contains(content.modelId())) {
      throw new AiPromptRuleException(
          AiPromptRuleException.MODEL_NOT_ALLOWED, "Model is not on the allowed list");
    }
    templates.lock(command.feature());
    return templates
        .find(command.feature(), PromptStatus.DRAFT)
        .map(draft -> templates.updateDraft(draft.id(), content))
        .orElseGet(() -> templates.createDraft(command.feature(), content, command.actor()));
  }
}
