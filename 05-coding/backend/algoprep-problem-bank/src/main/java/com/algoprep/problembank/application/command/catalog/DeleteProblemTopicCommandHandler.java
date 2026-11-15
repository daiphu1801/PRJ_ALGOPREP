package com.algoprep.problembank.application.command.catalog;

import com.algoprep.problembank.application.dto.catalog.CatalogCommand;
import com.algoprep.problembank.application.ports.in.catalog.DeleteProblemTopicUseCase;
import com.algoprep.problembank.domain.model.catalog.CatalogItem;
import com.algoprep.problembank.domain.model.catalog.CatalogKind;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DeleteProblemTopicCommandHandler implements DeleteProblemTopicUseCase {
  private final CatalogMutationService service;

  DeleteProblemTopicCommandHandler(CatalogMutationService service) {
    this.service = service;
  }

  @Override
  @Transactional
  public List<CatalogItem> execute(CatalogCommand command) {
    return service.delete(CatalogKind.TOPIC, command);
  }
}
