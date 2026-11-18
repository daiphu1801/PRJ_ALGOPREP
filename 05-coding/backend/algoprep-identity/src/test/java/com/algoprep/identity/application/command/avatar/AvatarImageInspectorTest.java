package com.algoprep.identity.application.command.avatar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.PayloadTooLargeException;
import com.algoprep.identity.domain.model.avatar.AvatarImage;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** DD validation F1 section 15 rules 2 to 4: size, magic bytes, header-only pixel size. */
class AvatarImageInspectorTest {

  private static byte[] png(int width, int height) {
    byte[] d = new byte[40];
    byte[] magic = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    System.arraycopy(magic, 0, d, 0, 8);
    d[11] = 13;
    d[12] = 'I';
    d[13] = 'H';
    d[14] = 'D';
    d[15] = 'R';
    putBe32(d, 16, width);
    putBe32(d, 20, height);
    return d;
  }

  private static void putBe32(byte[] d, int at, int v) {
    d[at] = (byte) (v >>> 24);
    d[at + 1] = (byte) (v >>> 16);
    d[at + 2] = (byte) (v >>> 8);
    d[at + 3] = (byte) v;
  }

  /** SOI, a 16-byte APP0 segment, then SOF0 (or the given marker). */
  private static byte[] jpeg(int marker, int width, int height) {
    byte[] d = new byte[48];
    d[0] = (byte) 0xFF;
    d[1] = (byte) 0xD8;
    d[2] = (byte) 0xFF;
    d[3] = (byte) 0xE0;
    d[5] = 16;
    d[20] = (byte) 0xFF;
    d[21] = (byte) marker;
    d[23] = 17;
    d[24] = 8;
    d[25] = (byte) (height >> 8);
    d[26] = (byte) height;
    d[27] = (byte) (width >> 8);
    d[28] = (byte) width;
    return d;
  }

  private static byte[] webp(String chunk) {
    byte[] d = new byte[40];
    System.arraycopy("RIFF".getBytes(StandardCharsets.US_ASCII), 0, d, 0, 4);
    System.arraycopy("WEBP".getBytes(StandardCharsets.US_ASCII), 0, d, 8, 4);
    System.arraycopy(chunk.getBytes(StandardCharsets.US_ASCII), 0, d, 12, 4);
    return d;
  }

  private static byte[] webpLossy(int width, int height) {
    byte[] d = webp("VP8 ");
    d[23] = (byte) 0x9D;
    d[24] = 0x01;
    d[25] = 0x2A;
    d[26] = (byte) width;
    d[27] = (byte) (width >> 8);
    d[28] = (byte) height;
    d[29] = (byte) (height >> 8);
    return d;
  }

  private static byte[] webpLossless(int width, int height) {
    byte[] d = webp("VP8L");
    d[20] = 0x2F;
    int bits = (width - 1) | ((height - 1) << 14);
    d[21] = (byte) bits;
    d[22] = (byte) (bits >> 8);
    d[23] = (byte) (bits >> 16);
    d[24] = (byte) (bits >> 24);
    return d;
  }

  private static byte[] webpExtended(int width, int height) {
    byte[] d = webp("VP8X");
    int w = width - 1;
    int h = height - 1;
    d[24] = (byte) w;
    d[25] = (byte) (w >> 8);
    d[26] = (byte) (w >> 16);
    d[27] = (byte) h;
    d[28] = (byte) (h >> 8);
    d[29] = (byte) (h >> 16);
    return d;
  }

  private static void assertRule(
      byte[] data, Class<? extends AlgoPrepException> type, String code) {
    assertThatThrownBy(() -> AvatarImageInspector.inspect(data))
        .isInstanceOf(type)
        .satisfies(e -> assertThat(((AlgoPrepException) e).errorCode()).isEqualTo(code));
  }

  @Test
  void aPngIsReadFromItsHeader() {
    assertThat(AvatarImageInspector.inspect(png(256, 128)))
        .isEqualTo(new AvatarImage("image/png", "png", 256, 128));
  }

  @Test
  void aJpegIsReadFromTheFirstFrameMarker() {
    assertThat(AvatarImageInspector.inspect(jpeg(0xC0, 300, 200)))
        .isEqualTo(new AvatarImage("image/jpeg", "jpg", 300, 200));
    assertThat(AvatarImageInspector.inspect(jpeg(0xC2, 64, 32)).width()).isEqualTo(64);
  }

  @Test
  void aJpegSkipsFillBytesAndStandaloneMarkersBeforeTheFrame() {
    byte[] d = jpeg(0xC0, 10, 20);
    // Replace the APP0 segment by a fill byte, a standalone RST marker and padding fill bytes.
    Arrays.fill(d, 2, 20, (byte) 0xFF);
    d[2] = (byte) 0xFF;
    d[3] = (byte) 0xD0;
    assertThat(AvatarImageInspector.inspect(d).height()).isEqualTo(20);
  }

  @Test
  void aWebpIsReadForLossyLosslessAndExtendedChunks() {
    assertThat(AvatarImageInspector.inspect(webpLossy(200, 100)))
        .isEqualTo(new AvatarImage("image/webp", "webp", 200, 100));
    assertThat(AvatarImageInspector.inspect(webpLossless(1024, 1)))
        .isEqualTo(new AvatarImage("image/webp", "webp", 1024, 1));
    assertThat(AvatarImageInspector.inspect(webpExtended(512, 640)))
        .isEqualTo(new AvatarImage("image/webp", "webp", 512, 640));
  }

  @Test
  void theSideLimitIs1024InclusiveAndZeroIsRefused() {
    assertThat(AvatarImageInspector.inspect(png(1024, 1024)).width()).isEqualTo(1024);
    assertRule(png(1025, 10), DomainRuleViolationException.class, "IDT-V31");
    assertRule(png(10, 1025), DomainRuleViolationException.class, "IDT-V31");
    assertRule(png(0, 10), DomainRuleViolationException.class, "IDT-V31");
    assertRule(png(4000, 4000), DomainRuleViolationException.class, "IDT-V31");
    assertRule(png(Integer.MIN_VALUE, 10), DomainRuleViolationException.class, "IDT-V31");
    assertRule(jpeg(0xC0, 1025, 10), DomainRuleViolationException.class, "IDT-V31");
    assertRule(webpLossy(1025, 10), DomainRuleViolationException.class, "IDT-V31");
    assertRule(webpLossless(10, 1025), DomainRuleViolationException.class, "IDT-V31");
    assertRule(webpExtended(2000, 10), DomainRuleViolationException.class, "IDT-V31");
  }

  @Test
  void aHeaderThatCannotBeReadIsV31() {
    assertRule(Arrays.copyOf(png(10, 10), 20), DomainRuleViolationException.class, "IDT-V31");
    byte[] notIhdr = png(10, 10);
    notIhdr[12] = 'X';
    assertRule(notIhdr, DomainRuleViolationException.class, "IDT-V31");
    assertRule(
        new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
        DomainRuleViolationException.class,
        "IDT-V31");
    // Start of scan before any frame marker, and a non-marker byte where a marker must be.
    assertRule(jpeg(0xDA, 10, 10), DomainRuleViolationException.class, "IDT-V31");
    assertRule(jpeg(0xC4, 10, 10), DomainRuleViolationException.class, "IDT-V31");
    byte[] broken = jpeg(0xC0, 10, 10);
    broken[20] = 0x12;
    assertRule(broken, DomainRuleViolationException.class, "IDT-V31");
    assertRule(webp("VP8 "), DomainRuleViolationException.class, "IDT-V31");
    assertRule(webp("ABCD"), DomainRuleViolationException.class, "IDT-V31");
    assertRule(webp("VP8L"), DomainRuleViolationException.class, "IDT-V31");
    assertRule(Arrays.copyOf(webpLossy(10, 10), 20), DomainRuleViolationException.class, "IDT-V31");
  }

  @Test
  void anythingThatIsNotPngJpegOrWebpIsV29() {
    byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(StandardCharsets.UTF_8);
    assertRule(svg, DomainRuleViolationException.class, "IDT-V29");
    assertRule(
        "just text named .png".getBytes(StandardCharsets.UTF_8),
        DomainRuleViolationException.class,
        "IDT-V29");
    assertRule(new byte[0], DomainRuleViolationException.class, "IDT-V29");
    assertRule(
        new byte[] {(byte) 0xFF, (byte) 0xD8}, DomainRuleViolationException.class, "IDT-V29");
    assertRule(
        "GIF89a....".getBytes(StandardCharsets.US_ASCII),
        DomainRuleViolationException.class,
        "IDT-V29");
    byte[] riffNotWebp = webpLossy(10, 10);
    riffNotWebp[8] = 'A';
    assertRule(riffNotWebp, DomainRuleViolationException.class, "IDT-V29");
    byte[] almostPng = png(10, 10);
    almostPng[7] = 0x0B;
    assertRule(almostPng, DomainRuleViolationException.class, "IDT-V29");
  }

  @Test
  void theSizeLimitIs512KbInclusiveAndIsCheckedFirst() {
    byte[] atLimit = Arrays.copyOf(png(10, 10), 524_288);
    assertThat(AvatarImageInspector.inspect(atLimit).contentType()).isEqualTo("image/png");
    assertRule(Arrays.copyOf(png(10, 10), 524_289), PayloadTooLargeException.class, "IDT-V30");
    // Oversized garbage is a size problem, not a format problem.
    assertRule(new byte[600 * 1024], PayloadTooLargeException.class, "IDT-V30");
  }
}
