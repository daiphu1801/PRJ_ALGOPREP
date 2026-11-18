package com.algoprep.bootstrap.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Dedicated scrape credential, separate from end-user JWTs. Empty configuration denies access. */
final class MonitoringToken {
  private MonitoringToken() {}

  static boolean matches(String expected, String authorization) {
    return expected != null
        && !expected.isBlank()
        && authorization != null
        && authorization.startsWith("Bearer ")
        && MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            authorization.substring(7).getBytes(StandardCharsets.UTF_8));
  }
}
