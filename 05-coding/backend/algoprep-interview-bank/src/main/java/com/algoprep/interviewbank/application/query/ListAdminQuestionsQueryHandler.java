package com.algoprep.interviewbank.application.query;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.interviewbank.application.dto.AdminQuestionQuery;
import com.algoprep.interviewbank.application.ports.in.ListAdminQuestionsUseCase;
import com.algoprep.interviewbank.domain.model.AdminQuestionPage;
import com.algoprep.interviewbank.domain.model.CatalogItem;
import com.algoprep.interviewbank.domain.model.CatalogKind;
import com.algoprep.interviewbank.domain.model.QuestionListCriteria;
import com.algoprep.interviewbank.domain.model.QuestionSearch;
import com.algoprep.interviewbank.domain.ports.out.AdminQuestionRepository;
import com.algoprep.interviewbank.domain.ports.out.CatalogRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Strict reader (unknown topic or level is INB-V01); search follows BR-IB-04 without keywords. */
@Service
class ListAdminQuestionsQueryHandler implements ListAdminQuestionsUseCase {
  private final AdminQuestionRepository repository;
  private final CatalogRepository catalog;

  ListAdminQuestionsQueryHandler(AdminQuestionRepository repository, CatalogRepository catalog) {
    this.repository = repository;
    this.catalog = catalog;
  }

  @Override
  @Transactional(readOnly = true)
  public AdminQuestionPage list(AdminQuestionQuery query) {
    return repository.page(
        new QuestionListCriteria(
            QuestionSearch.tsQuery(query.q()),
            resolve(CatalogKind.TOPIC, query.topic()),
            resolve(CatalogKind.LEVEL, query.level()),
            query.sort(),
            query.descending(),
            query.page(),
            query.pageSize()));
  }

  private UUID resolve(CatalogKind kind, String code) {
    if (code == null) {
      return null;
    }
    return catalog.list(kind, false).stream()
        .filter(item -> item.code().equals(code))
        .map(CatalogItem::id)
        .findFirst()
        .orElseThrow(() -> new InvalidRequestException("INB-V01", "Unknown " + kind + " code"));
  }
}
