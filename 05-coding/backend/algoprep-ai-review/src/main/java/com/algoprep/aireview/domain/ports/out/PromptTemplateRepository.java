package com.algoprep.aireview.domain.ports.out;

import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.PromptContent;
import com.algoprep.aireview.domain.model.PromptStatus;
import com.algoprep.aireview.domain.model.PromptTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Admin view of {@code ai.prompt_templates} (BR-AI-13). Writes join the caller transaction. */
public interface PromptTemplateRepository {
  /** ACTIVE and DRAFT rows, features in fixed order, ACTIVE before DRAFT. */
  List<PromptTemplate> listLive();

  /** Every row of a feature, newest first. */
  List<PromptTemplate> history(FeatureCode feature);

  Optional<PromptTemplate> find(FeatureCode feature, PromptStatus status);

  /** Transaction-scoped lock that serialises draft saves and publishes of one feature. */
  void lock(FeatureCode feature);

  PromptTemplate createDraft(FeatureCode feature, PromptContent content, UUID actor);

  PromptTemplate updateDraft(UUID draftId, PromptContent content);

  void archive(UUID activeId);

  /** Turns a DRAFT into the ACTIVE row with the given content. */
  PromptTemplate promote(UUID draftId, PromptContent content);

  PromptTemplate createActive(FeatureCode feature, PromptContent content, UUID actor);
}
