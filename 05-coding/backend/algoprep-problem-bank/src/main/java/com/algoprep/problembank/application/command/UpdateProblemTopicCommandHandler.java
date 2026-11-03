package com.algoprep.problembank.application.command;

import com.algoprep.problembank.application.dto.CatalogCommand;
import com.algoprep.problembank.application.ports.in.UpdateProblemTopicUseCase;
import com.algoprep.problembank.domain.model.CatalogItem;
import com.algoprep.problembank.domain.model.CatalogKind;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class UpdateProblemTopicCommandHandler implements UpdateProblemTopicUseCase {
  private final CatalogMutationService service;

  UpdateProblemTopicCommandHandler(CatalogMutationService service) {
    this.service = service;
  }

  @Override
  @Transactional
  public List<CatalogItem> execute(CatalogCommand command) {
    return service.update(CatalogKind.TOPIC, command);
  }
}
