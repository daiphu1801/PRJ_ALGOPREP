package com.algoprep.aireview.domain.llm;

/**
 * Cuts the visible {@code message} text out of a streamed turn reply {@code {"message": "...",
 * "stageComplete": bool}} while the JSON is still arriving (BR-AI-07 step 8). It decodes JSON
 * escapes and returns, for each fed chunk, the message characters that became known. It only
 * streams when {@code message} is the FIRST key; with another key first, or anything it does not
 * recognise, it stays silent for the rest of the reply ({@link #streamedAnything()} stays false)
 * and the caller falls back to one token built from the fully parsed reply. {@code stageComplete}
 * can therefore never leave the server as raw text. Not thread-safe; one instance per reply.
 */
public final class TurnMessageCutter {
  private static final String KEY = "\"message\"";
  private static final int SEEK_OPEN_BRACE = 0;
  private static final int SEEK_KEY = 1;
  private static final int SEEK_COLON = 2;
  private static final int SEEK_QUOTE = 3;
  private static final int STREAMING = 4;
  private static final int DONE = 5;
  private static final int OFF = 6;
  private static final int HEX_DIGITS = 4;

  private int phase = SEEK_OPEN_BRACE;
  private int keyMatched;
  private boolean inEscape;
  private final StringBuilder unicode = new StringBuilder();
  private char heldHighSurrogate;
  private boolean held;
  private boolean streamed;

  /** The newly decoded message text for this chunk; empty when there is nothing new to show. */
  public String feed(String chunk) {
    if (chunk == null || chunk.isEmpty() || phase == DONE || phase == OFF) {
      return "";
    }
    StringBuilder out = new StringBuilder();
    if (held) {
      // The pair's first half was held back by the previous chunk; it goes out first.
      out.append(heldHighSurrogate);
      held = false;
    }
    int start = 0;
    for (int i = 0; i < chunk.length() && phase != DONE && phase != OFF; i++) {
      step(chunk.charAt(i), out);
    }
    if (phase == STREAMING
        && out.length() > start
        && Character.isHighSurrogate(out.charAt(out.length() - 1))) {
      heldHighSurrogate = out.charAt(out.length() - 1);
      held = true;
      out.setLength(out.length() - 1);
    }
    if (!out.isEmpty()) {
      streamed = true;
    }
    return out.toString();
  }

  /** True once at least one message character was returned; the caller then must not re-emit. */
  public boolean streamedAnything() {
    return streamed;
  }

  private void step(char c, StringBuilder out) {
    switch (phase) {
      case SEEK_OPEN_BRACE -> {
        if (c == '{') {
          phase = SEEK_KEY;
        }
      }
      case SEEK_KEY -> seekKey(c);
      case SEEK_COLON -> {
        if (c == ':') {
          phase = SEEK_QUOTE;
        } else if (!Character.isWhitespace(c)) {
          phase = OFF;
        }
      }
      case SEEK_QUOTE -> {
        if (c == '"') {
          phase = STREAMING;
        } else if (!Character.isWhitespace(c)) {
          phase = OFF;
        }
      }
      default -> streamChar(c, out);
    }
  }

  private void seekKey(char c) {
    if (keyMatched == 0 && Character.isWhitespace(c)) {
      return;
    }
    if (c != KEY.charAt(keyMatched)) {
      phase = OFF;
      return;
    }
    keyMatched++;
    if (keyMatched == KEY.length()) {
      phase = SEEK_COLON;
    }
  }

  private void streamChar(char c, StringBuilder out) {
    if (!unicode.isEmpty() || (inEscape && c == 'u')) {
      unicodeDigit(c, out);
    } else if (inEscape) {
      inEscape = false;
      out.append(simpleEscape(c));
    } else if (c == '\\') {
      inEscape = true;
    } else if (c == '"') {
      phase = DONE;
    } else {
      out.append(c);
    }
  }

  private void unicodeDigit(char c, StringBuilder out) {
    if (inEscape) {
      // The 'u' itself; the four hex digits follow.
      inEscape = false;
      unicode.append('u');
      return;
    }
    unicode.append(c);
    if (unicode.length() == HEX_DIGITS + 1) {
      boolean hex = unicode.substring(1).chars().allMatch(d -> Character.digit(d, 16) >= 0);
      if (hex) {
        out.append((char) Integer.parseInt(unicode.substring(1), 16));
      } else {
        phase = OFF;
      }
      unicode.setLength(0);
    }
  }

  private static char simpleEscape(char c) {
    return switch (c) {
      case 'n' -> '\n';
      case 't' -> '\t';
      case 'r' -> '\r';
      case 'b' -> '\b';
      case 'f' -> '\f';
      default -> c;
    };
  }
}
