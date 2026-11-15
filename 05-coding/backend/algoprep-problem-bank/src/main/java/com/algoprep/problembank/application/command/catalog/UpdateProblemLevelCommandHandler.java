package com.algoprep.problembank.application.command.catalog;

import com.algoprep.problembank.application.dto.catalog.CatalogCommand;
import com.algoprep.problembank.application.ports.in.catalog.UpdateProblemLevelUseCase;
import com.algoprep.problembank.domain.model.catalog.CatalogItem;
import com.algoprep.problembank.domain.model.catalog.CatalogKind;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class UpdateProblemLevelCommandHandler implements UpdateProblemLevelUseCase {
  private final CatalogMutationService service;

  UpdateProblemLevelCommandHandler(CatalogMutationService service) {
    this.service = service;
  }

  @Override
  @Transactional
  public List<CatalogItem> execute(CatalogCommand command) {
    return service.update(CatalogKind.LEVEL, command);
  }
}
