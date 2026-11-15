package com.algoprep.identity.domain.model;

import java.nio.charset.StandardCharsets;

/** BCrypt input boundary approved in DEC-2026-1030-bcrypt-password-byte-limit. */
public final class PasswordPolicy {
  public static final int MAX_UTF8_BYTES = 72;

  private PasswordPolicy() {}

  /** The boundary is bytes, so non-ASCII passwords may reach it before 72 characters. */
  public static boolean fitsBcrypt(String password) {
    return password != null && password.getBytes(StandardCharsets.UTF_8).length <= MAX_UTF8_BYTES;
  }
}
