package com.algoprep.interviewbank.application.query;

import com.algoprep.interviewbank.application.dto.CatalogQuery;
import com.algoprep.interviewbank.application.ports.in.ListQuestionLevelsUseCase;
import com.algoprep.interviewbank.domain.model.CatalogItem;
import com.algoprep.interviewbank.domain.model.CatalogKind;
import com.algoprep.interviewbank.domain.ports.out.CatalogRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ListQuestionLevelsQueryHandler implements ListQuestionLevelsUseCase {
  private final CatalogRepository repository;

  ListQuestionLevelsQueryHandler(CatalogRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<CatalogItem> list(CatalogQuery query) {
    return repository.list(CatalogKind.LEVEL, query.admin() && query.includeCounts());
  }
}
