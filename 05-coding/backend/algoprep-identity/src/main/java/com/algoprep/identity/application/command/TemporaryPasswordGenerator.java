package com.algoprep.identity.application.command;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * A 12 character temporary password that always satisfies the password policy: letters and digits
 * (look-alike characters left out), at least one of each, ASCII so it is far below 72 bytes. The
 * admin never sees it; it only travels by e-mail (BR-21).
 */
@Component
class TemporaryPasswordGenerator {
  private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz";
  private static final String DIGITS = "23456789";
  private static final String ALL = LETTERS + DIGITS;
  private static final int LENGTH = 12;
  private final SecureRandom random = new SecureRandom();

  String generate() {
    char[] out = new char[LENGTH];
    out[0] = pick(LETTERS);
    out[1] = pick(DIGITS);
    for (int i = 2; i < LENGTH; i++) {
      out[i] = pick(ALL);
    }
    for (int i = LENGTH - 1; i > 0; i--) { // Fisher-Yates, so the guaranteed ones move around
      int j = random.nextInt(i + 1);
      char swap = out[i];
      out[i] = out[j];
      out[j] = swap;
    }
    return new String(out);
  }

  private char pick(String alphabet) {
    return alphabet.charAt(random.nextInt(alphabet.length()));
  }
}
