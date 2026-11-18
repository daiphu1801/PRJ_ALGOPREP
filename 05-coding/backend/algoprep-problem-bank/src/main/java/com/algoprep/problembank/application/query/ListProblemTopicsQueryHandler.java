package com.algoprep.problembank.application.query;

import com.algoprep.problembank.application.dto.catalog.CatalogQuery;
import com.algoprep.problembank.application.ports.in.catalog.ListProblemTopicsUseCase;
import com.algoprep.problembank.domain.model.catalog.CatalogItem;
import com.algoprep.problembank.domain.model.catalog.CatalogKind;
import com.algoprep.problembank.domain.ports.out.CatalogRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ListProblemTopicsQueryHandler implements ListProblemTopicsUseCase {
  private final CatalogRepository repository;

  ListProblemTopicsQueryHandler(CatalogRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<CatalogItem> list(CatalogQuery query) {
    return repository.list(CatalogKind.TOPIC, query.admin() && query.includeCounts());
  }
}
