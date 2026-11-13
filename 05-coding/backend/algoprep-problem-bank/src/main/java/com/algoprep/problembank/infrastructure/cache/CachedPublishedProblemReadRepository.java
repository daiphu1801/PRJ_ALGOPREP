package com.algoprep.problembank.infrastructure.cache;

import com.algoprep.problembank.domain.model.PublishedProblemModels.Filter;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Overview;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Page;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Paging;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Samples;
import com.algoprep.problembank.domain.model.PublishedProblemModels.SolveStates;
import com.algoprep.problembank.domain.model.PublishedProblemModels.Workspace;
import com.algoprep.problembank.domain.ports.out.PublishedProblemReadRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** Shared pages contain no user state. Status-filtered pages always go to the origin. */
@Repository
@Primary
class CachedPublishedProblemReadRepository implements PublishedProblemReadRepository {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private final PublishedProblemReadRepository origin;
  private final CatalogReadCache cache;

  CachedPublishedProblemReadRepository(
      @Qualifier("jdbcPublishedProblemReadRepository") PublishedProblemReadRepository origin,
      CatalogReadCache cache) {
    this.origin = origin;
    this.cache = cache;
  }

  @Override
  public Page list(Filter filter, SolveStates states, Paging paging) {
    if (filter.status() != null) {
      return origin.list(filter, states, paging);
    }
    String key = "problem:list:v" + cache.version() + ":" + hash(filter, paging);
    return cache.read(key, Page.class, () -> origin.list(filter, null, paging));
  }

  @Override
  public Optional<UUID> random(Filter filter, SolveStates states) {
    return origin.random(filter, states);
  }

  @Override
  public Optional<Workspace> workspace(UUID id, UUID user) {
    return origin.workspace(id, user);
  }

  @Override
  public Optional<Overview> overview(UUID id, UUID user) {
    return origin.overview(id, user);
  }

  @Override
  public Optional<Samples> samples(UUID id) {
    return origin.samples(id);
  }

  private static String hash(Filter filter, Paging paging) {
    try {
      String body = JSON.writeValueAsString(filter) + "\n" + JSON.writeValueAsString(paging);
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }
}
