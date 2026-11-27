package com.algoprep.interviewbank.application.command;

import com.algoprep.interviewbank.application.dto.CatalogCommand;
import com.algoprep.interviewbank.application.ports.in.ReorderQuestionLevelsUseCase;
import com.algoprep.interviewbank.domain.model.CatalogItem;
import com.algoprep.interviewbank.domain.model.CatalogKind;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReorderQuestionLevelsCommandHandler implements ReorderQuestionLevelsUseCase {
  private final QuestionCatalogMutationService service;

  ReorderQuestionLevelsCommandHandler(QuestionCatalogMutationService service) {
    this.service = service;
  }

  @Override
  @Transactional
  public List<CatalogItem> execute(CatalogCommand command) {
    return service.reorder(CatalogKind.LEVEL, command);
  }
}
