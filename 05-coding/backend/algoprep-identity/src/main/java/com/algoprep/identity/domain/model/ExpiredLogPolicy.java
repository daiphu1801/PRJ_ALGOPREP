package com.algoprep.identity.domain.model;

/** What the purge job does with audit rows older than the retention (DD api F1 section 0.7.1). */
public enum ExpiredLogPolicy {
  /** Delete the rows in batches. */
  DELETE,
  /** Compress and archive: deferred, the job keeps the rows and logs a warning. */
  ARCHIVE
}
