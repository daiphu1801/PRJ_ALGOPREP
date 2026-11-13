package com.algoprep.problembank.infrastructure.cache;

import com.algoprep.problembank.domain.model.PublishedProblemModels.SolveStates;
import com.algoprep.problembank.domain.model.StudentCatalogModels.CatalogSummary;
import com.algoprep.problembank.domain.model.StudentCatalogModels.LevelProgress;
import com.algoprep.problembank.domain.model.StudentCatalogModels.TopicProgress;
import com.algoprep.problembank.domain.ports.out.StudentCatalogReadRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
class CachedStudentCatalogReadRepository implements StudentCatalogReadRepository {
  private final StudentCatalogReadRepository origin;
  private final CatalogReadCache cache;

  CachedStudentCatalogReadRepository(
      @Qualifier("jdbcStudentCatalogReadRepository") StudentCatalogReadRepository origin,
      CatalogReadCache cache) {
    this.origin = origin;
    this.cache = cache;
  }

  @Override
  public List<TopicProgress> topicProgress(SolveStates states) {
    return origin.topicProgress(states);
  }

  @Override
  public List<LevelProgress> levelProgress(SolveStates states) {
    return origin.levelProgress(states);
  }

  @Override
  public CatalogSummary summary() {
    return cache.read("problem:summary:v" + cache.version(), CatalogSummary.class, origin::summary);
  }
}
