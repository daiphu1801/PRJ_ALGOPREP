package com.algoprep.problembank.application.command;

import com.algoprep.problembank.application.dto.CatalogCommand;
import com.algoprep.problembank.application.ports.in.DeleteProblemLevelUseCase;
import com.algoprep.problembank.domain.model.CatalogItem;
import com.algoprep.problembank.domain.model.CatalogKind;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DeleteProblemLevelCommandHandler implements DeleteProblemLevelUseCase {
  private final CatalogMutationService service;

  DeleteProblemLevelCommandHandler(CatalogMutationService service) {
    this.service = service;
  }

  @Override
  @Transactional
  public List<CatalogItem> execute(CatalogCommand command) {
    return service.delete(CatalogKind.LEVEL, command);
  }
}
