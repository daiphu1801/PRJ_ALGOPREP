package com.algoprep.interviewbank.application.query;

import com.algoprep.interviewbank.application.dto.CatalogQuery;
import com.algoprep.interviewbank.application.ports.in.ListQuestionTopicsUseCase;
import com.algoprep.interviewbank.domain.model.CatalogItem;
import com.algoprep.interviewbank.domain.model.CatalogKind;
import com.algoprep.interviewbank.domain.ports.out.CatalogRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ListQuestionTopicsQueryHandler implements ListQuestionTopicsUseCase {
  private final CatalogRepository repository;

  ListQuestionTopicsQueryHandler(CatalogRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<CatalogItem> list(CatalogQuery query) {
    return repository.list(CatalogKind.TOPIC, query.admin() && query.includeCounts());
  }
}
