package com.algoprep.problembank.application.command.catalog;

import com.algoprep.problembank.application.dto.catalog.CatalogCommand;
import com.algoprep.problembank.application.ports.in.catalog.ReorderProblemLevelsUseCase;
import com.algoprep.problembank.domain.model.catalog.CatalogItem;
import com.algoprep.problembank.domain.model.catalog.CatalogKind;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReorderProblemLevelsCommandHandler implements ReorderProblemLevelsUseCase {
  private final CatalogMutationService service;

  ReorderProblemLevelsCommandHandler(CatalogMutationService service) {
    this.service = service;
  }

  @Override
  @Transactional
  public List<CatalogItem> execute(CatalogCommand command) {
    return service.reorder(CatalogKind.LEVEL, command);
  }
}
