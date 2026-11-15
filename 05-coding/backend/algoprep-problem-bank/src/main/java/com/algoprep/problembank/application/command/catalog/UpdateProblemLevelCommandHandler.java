package com.algoprep.problembank.application.command;

import com.algoprep.problembank.application.dto.CatalogCommand;
import com.algoprep.problembank.application.ports.in.UpdateProblemLevelUseCase;
import com.algoprep.problembank.domain.model.CatalogItem;
import com.algoprep.problembank.domain.model.CatalogKind;
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
