package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.application.dto.PromptTemplateList;
import com.algoprep.aireview.application.dto.PublishChange;
import java.util.List;
import java.util.UUID;

public interface PublishPromptTemplatesUseCase {
  PromptTemplateList publish(UUID actor, List<PublishChange> changes);
}
