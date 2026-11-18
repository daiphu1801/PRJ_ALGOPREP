package com.algoprep.problembank.application.ports.out;

import java.time.Duration;

/** Internal storage only. Callers must authorize visibility before requesting any signed URL. */
public interface TestcaseObjectStorePort {
  String presignPut(String key, Duration ttl);

  String presignGet(String key, Duration ttl);

  long head(String key);

  byte[] get(String key);

  void put(String key, byte[] bytes);

  void copy(String source, String destination);

  void delete(String key);
}
