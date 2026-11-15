package com.algoprep.problembank.application.command.catalog;

import com.algoprep.problembank.application.dto.catalog.CatalogCommand;
import com.algoprep.problembank.application.ports.in.catalog.CreateProblemLevelUseCase;
import com.algoprep.problembank.domain.model.catalog.CatalogItem;
import com.algoprep.problembank.domain.model.catalog.CatalogKind;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CreateProblemLevelCommandHandler implements CreateProblemLevelUseCase {
  private final CatalogMutationService service;

  CreateProblemLevelCommandHandler(CatalogMutationService service) {
    this.service = service;
  }

  @Override
  @Transactional
  public List<CatalogItem> execute(CatalogCommand command) {
    return service.create(CatalogKind.LEVEL, command);
  }
}
