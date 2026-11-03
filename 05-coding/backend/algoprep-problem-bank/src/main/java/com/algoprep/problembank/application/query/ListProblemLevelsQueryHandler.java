package com.algoprep.problembank.application.query;

import com.algoprep.problembank.application.dto.CatalogQuery;
import com.algoprep.problembank.application.ports.in.ListProblemLevelsUseCase;
import com.algoprep.problembank.domain.model.CatalogItem;
import com.algoprep.problembank.domain.model.CatalogKind;
import com.algoprep.problembank.domain.ports.out.CatalogRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ListProblemLevelsQueryHandler implements ListProblemLevelsUseCase {
  private final CatalogRepository repository;

  ListProblemLevelsQueryHandler(CatalogRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<CatalogItem> list(CatalogQuery query) {
    return repository.list(CatalogKind.LEVEL, query.admin() && query.includeCounts());
  }
}
