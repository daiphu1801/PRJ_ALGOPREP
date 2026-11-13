package com.algoprep.interviewbank.application.query;

import com.algoprep.interviewbank.application.dto.LearnerQuestionQuery;
import com.algoprep.interviewbank.application.ports.in.ListInterviewQuestionsUseCase;
import com.algoprep.interviewbank.domain.model.CatalogItem;
import com.algoprep.interviewbank.domain.model.CatalogKind;
import com.algoprep.interviewbank.domain.model.LearnerQuestionCriteria;
import com.algoprep.interviewbank.domain.model.LearnerQuestionPage;
import com.algoprep.interviewbank.domain.model.QuestionSearch;
import com.algoprep.interviewbank.domain.ports.out.CatalogRepository;
import com.algoprep.interviewbank.domain.ports.out.LearnerQuestionRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lenient reader (BR-IB-04): an unknown topic or level code is ignored, never an error. */
@Service
class ListInterviewQuestionsQueryHandler implements ListInterviewQuestionsUseCase {
  static final int PAGE_SIZE = 20;
  static final int MAX_QUERY = 100;

  private final LearnerQuestionRepository repository;
  private final CatalogRepository catalog;

  ListInterviewQuestionsQueryHandler(
      LearnerQuestionRepository repository, CatalogRepository catalog) {
    this.repository = repository;
    this.catalog = catalog;
  }

  @Override
  @Transactional(readOnly = true)
  public LearnerQuestionPage list(LearnerQuestionQuery query) {
    String text = truncate(query.q());
    String tsQuery = QuestionSearch.tsQuery(text);
    return repository.page(
        new LearnerQuestionCriteria(
            query.userId(),
            tsQuery,
            tsQuery == null ? null : QuestionSearch.escapeLike(text),
            resolve(CatalogKind.TOPIC, query.topic()),
            resolve(CatalogKind.LEVEL, query.level()),
            query.status(),
            query.bookmarkedOnly(),
            Math.max(1, query.page()),
            query.drill() ? Math.max(1, Math.min(20, query.limit())) : PAGE_SIZE,
            query.drill()));
  }

  private static String truncate(String q) {
    if (q == null) {
      return null;
    }
    String text = q.strip();
    int length = text.codePointCount(0, text.length());
    return length <= MAX_QUERY ? text : text.substring(0, text.offsetByCodePoints(0, MAX_QUERY));
  }

  private UUID resolve(CatalogKind kind, String code) {
    if (code == null) {
      return null;
    }
    return catalog.list(kind, false).stream()
        .filter(item -> item.code().equals(code))
        .map(CatalogItem::id)
        .findFirst()
        .orElse(null);
  }
}
