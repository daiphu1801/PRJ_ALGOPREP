package com.algoprep.problembank.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.problembank.domain.model.problem.ProblemContentData;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class ProblemCodeCollisionPgTest extends PostgresTestBase {
  private JdbcProblemContentRepository repository() {
    return new JdbcProblemContentRepository(
        provider(),
        UUID::randomUUID,
        new JdbcProblemContentRelations(provider(), UUID::randomUUID));
  }

  private static ProblemContentData draft(String title) {
    return new ProblemContentData(
        title,
        null,
        "",
        EASY,
        new ProblemContentData.Limits(1000, 256, 64, null),
        List.of(),
        List.of(),
        List.of(),
        null);
  }

  @Test
  void moreThanTwentyIdenticalTitlesAreUniqueAndDeletedCodesAreNotReused() {
    var repository = repository();
    var content = draft("Repeated title " + "long ".repeat(20));
    var ids = new ArrayList<UUID>();
    for (int i = 0; i < 25; i++) {
      ids.add(repository.create(AUTHOR, content));
    }
    var before = jdbc.sql("SELECT code FROM problem.problems").query(String.class).list();
    softDelete(ids.getFirst());
    UUID added = repository.create(AUTHOR, content);
    var after = jdbc.sql("SELECT code FROM problem.problems").query(String.class).list();
    assertThat(ids).doesNotHaveDuplicates();
    assertThat(after).hasSize(26).doesNotHaveDuplicates().containsAll(before);
    assertThat(after).allSatisfy(code -> assertThat(code.length()).isLessThanOrEqualTo(64));
    assertThat(
            jdbc.sql("SELECT code FROM problem.problems WHERE id=:id")
                .param("id", added)
                .query(String.class)
                .single())
        .endsWith(added.toString().toUpperCase(java.util.Locale.ROOT));
  }

  @Test
  void concurrentAuthorsCanCreateAfterAllNumericCandidatesAreTaken() throws Exception {
    var repository = repository();
    var content = draft("Concurrent collision");
    for (int i = 0; i < 20; i++) {
      repository.create(AUTHOR, content);
    }
    var jobs = new ArrayList<Callable<UUID>>();
    for (int i = 0; i < 12; i++) {
      UUID author = id(910 + i);
      jobs.add(() -> repository.create(author, content));
    }
    var created = new ArrayList<UUID>();
    try (var executor = Executors.newFixedThreadPool(4)) {
      for (var result : executor.invokeAll(jobs)) {
        created.add(result.get());
      }
    }
    assertThat(created).hasSize(12).doesNotHaveDuplicates();
    assertThat(jdbc.sql("SELECT code FROM problem.problems").query(String.class).list())
        .hasSize(32)
        .doesNotHaveDuplicates();
  }
}
