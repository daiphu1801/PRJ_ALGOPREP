package com.algoprep.identity.application.command.avatar;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.PayloadTooLargeException;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.avatar.AvatarImage;

/**
 * File checks of #74 in the order of DD validation F1 section 15: size, then magic bytes, then the
 * pixel size read from the header only. The image is never decoded, so a crafted file cannot cost
 * more than a few array reads.
 */
final class AvatarImageInspector {
  static final int MAX_BYTES = 524_288;
  static final int MAX_SIDE = 1024;

  private static final byte[] PNG = {
    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
  };

  private AvatarImageInspector() {}

  static AvatarImage inspect(byte[] data) {
    if (data.length > MAX_BYTES) {
      throw new PayloadTooLargeException(
          IdentityErrorCodes.AVATAR_TOO_LARGE, "Avatar is larger than 512 KB");
    }
    if (startsWith(data, PNG)) {
      return image("image/png", "png", pngSize(data));
    }
    if (data.length >= 3 && u8(data, 0) == 0xFF && u8(data, 1) == 0xD8 && u8(data, 2) == 0xFF) {
      return image("image/jpeg", "jpg", jpegSize(data));
    }
    if (isWebp(data)) {
      return image("image/webp", "webp", webpSize(data));
    }
    throw new DomainRuleViolationException(
        IdentityErrorCodes.AVATAR_FORMAT_INVALID, "Avatar is not a PNG, JPEG or WebP image");
  }

  private static AvatarImage image(String contentType, String extension, int[] size) {
    if (size == null || !inRange(size[0]) || !inRange(size[1])) {
      throw new DomainRuleViolationException(
          IdentityErrorCodes.AVATAR_DIMENSIONS_INVALID,
          "Avatar pixel size is unreadable or over 1024 x 1024");
    }
    return new AvatarImage(contentType, extension, size[0], size[1]);
  }

  private static boolean inRange(int side) {
    return side >= 1 && side <= MAX_SIDE;
  }

  /** IHDR is the first chunk: width and height are big-endian at bytes 16 to 23. */
  private static int[] pngSize(byte[] d) {
    if (d.length < 24 || d[12] != 'I' || d[13] != 'H' || d[14] != 'D' || d[15] != 'R') {
      return null;
    }
    return new int[] {be32(d, 16), be32(d, 20)};
  }

  /** Walks the marker segments up to the first start-of-frame marker. */
  private static int[] jpegSize(byte[] d) {
    int i = 2;
    while (i + 3 < d.length) {
      if (u8(d, i) != 0xFF) {
        return null;
      }
      int marker = u8(d, i + 1);
      if (marker == 0xFF) {
        i++;
        continue;
      }
      if (isFrameMarker(marker)) {
        return i + 8 < d.length ? new int[] {be16(d, i + 7), be16(d, i + 5)} : null;
      }
      if (marker == 0xD9 || marker == 0xDA) {
        return null;
      }
      boolean standalone = marker == 0x01 || (marker >= 0xD0 && marker <= 0xD8);
      i += standalone ? 2 : 2 + be16(d, i + 2);
    }
    return null;
  }

  private static boolean isFrameMarker(int marker) {
    return marker >= 0xC0 && marker <= 0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC;
  }

  private static boolean isWebp(byte[] d) {
    return d.length >= 12
        && d[0] == 'R'
        && d[1] == 'I'
        && d[2] == 'F'
        && d[3] == 'F'
        && d[8] == 'W'
        && d[9] == 'E'
        && d[10] == 'B'
        && d[11] == 'P';
  }

  private static int[] webpSize(byte[] d) {
    if (d.length < 30) {
      return null;
    }
    String chunk = new String(d, 12, 4, java.nio.charset.StandardCharsets.ISO_8859_1);
    return switch (chunk) {
      case "VP8 " ->
          u8(d, 23) == 0x9D && u8(d, 24) == 0x01 && u8(d, 25) == 0x2A
              ? new int[] {le16(d, 26) & 0x3FFF, le16(d, 28) & 0x3FFF}
              : null;
      case "VP8L" -> {
        if (u8(d, 20) != 0x2F) {
          yield null;
        }
        long bits = le16(d, 21) | ((long) le16(d, 23) << 16);
        yield new int[] {(int) (bits & 0x3FFF) + 1, (int) ((bits >> 14) & 0x3FFF) + 1};
      }
      case "VP8X" -> new int[] {le24(d, 24) + 1, le24(d, 27) + 1};
      default -> null;
    };
  }

  private static boolean startsWith(byte[] data, byte[] prefix) {
    if (data.length < prefix.length) {
      return false;
    }
    for (int i = 0; i < prefix.length; i++) {
      if (data[i] != prefix[i]) {
        return false;
      }
    }
    return true;
  }

  private static int u8(byte[] d, int i) {
    return d[i] & 0xFF;
  }

  private static int be16(byte[] d, int i) {
    return (u8(d, i) << 8) | u8(d, i + 1);
  }

  private static int be32(byte[] d, int i) {
    long v = ((long) be16(d, i) << 16) | be16(d, i + 2);
    // A side above Integer.MAX_VALUE is far over the limit; clamp so it never turns negative.
    return v > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) v;
  }

  private static int le16(byte[] d, int i) {
    return u8(d, i) | (u8(d, i + 1) << 8);
  }

  private static int le24(byte[] d, int i) {
    return le16(d, i) | (u8(d, i + 2) << 16);
  }
}
