package com.algoprep.judge.application.dto;

import java.util.Set;

/** DD #14: invalid tab/page falls back; a page beyond the end is clamped to the last. */
public record QueueJobsQuery(String tab, int page) {
  public QueueJobsQuery {
    tab = tab != null && Set.of("waiting", "failed", "all").contains(tab) ? tab : "waiting";
    page = Math.max(1, page);
  }

  public int resolvePage(long total, int pageSize) {
    long last = Math.max(1L, total / pageSize + (total % pageSize == 0 ? 0 : 1));
    return (int) Math.min(page, last);
  }

  public static QueueJobsQuery parse(String tab, String page) {
    try {
      return new QueueJobsQuery(tab, page == null ? 1 : Integer.parseInt(page));
    } catch (NumberFormatException failure) {
      return new QueueJobsQuery(tab, 1);
    }
  }
}
