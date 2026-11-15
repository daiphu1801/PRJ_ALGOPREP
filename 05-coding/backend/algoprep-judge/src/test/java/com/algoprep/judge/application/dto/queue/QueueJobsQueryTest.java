package com.algoprep.judge.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class QueueJobsQueryTest {
  @Test
  void unknownTabsAndMalformedPagesFallBackWithoutAcceptingAClientPageSize() {
    assertThat(QueueJobsQuery.parse(null, null)).isEqualTo(new QueueJobsQuery("waiting", 1));
    for (String page : List.of("", "bad", "1.5", "0", "-1", "2147483648")) {
      assertThat(QueueJobsQuery.parse("failed", page)).isEqualTo(new QueueJobsQuery("failed", 1));
    }
    for (String tab : List.of("ALL", "unknown", "all' OR true--")) {
      assertThat(QueueJobsQuery.parse(tab, "2")).isEqualTo(new QueueJobsQuery("waiting", 2));
    }
  }

  @Test
  void exactMultiplesPartialLastPagesAndEmptyQueuesResolveCorrectly() {
    var query = new QueueJobsQuery("all", 99);
    assertThat(query.resolvePage(0, 5)).isEqualTo(1);
    assertThat(query.resolvePage(10, 5)).isEqualTo(2);
    assertThat(query.resolvePage(11, 5)).isEqualTo(3);
    assertThat(new QueueJobsQuery("all", 1).resolvePage(11, 5)).isEqualTo(1);
  }
}
