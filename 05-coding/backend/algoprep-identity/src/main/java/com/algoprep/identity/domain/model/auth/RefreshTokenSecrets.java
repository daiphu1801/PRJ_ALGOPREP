package com.algoprep.identity.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Raw refresh token generation and hashing (BD security section 1, DD BR-02).
 *
 * <p>The raw value only travels in the HttpOnly cookie. The database stores the SHA-256 hash, so a
 * database leak does not hand out usable tokens.
 */
public final class RefreshTokenSecrets {

  private static final int TOKEN_BYTES = 32;
  private static final SecureRandom RANDOM = new SecureRandom();

  private RefreshTokenSecrets() {}

  /**
   * @return a new URL-safe random token with 256 bits of entropy
   */
  public static String newRawToken() {
    byte[] bytes = new byte[TOKEN_BYTES];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  /**
   * @param rawToken the token as sent by the client
   * @return lowercase hex SHA-256 of the UTF-8 bytes, the value stored in {@code
   *     refresh_tokens.token_hash}
   */
  public static String hash(String rawToken) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is required by the JDK specification", e);
    }
  }
}
