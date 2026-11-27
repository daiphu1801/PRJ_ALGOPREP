package com.algoprep.identity.infrastructure.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Every SSRF branch of BD security F1 section 6.5, with no DNS and no network. */
class HttpAvatarImageFetcherTest {
  private static final String GOOD = "https://avatars.githubusercontent.com/u/1?v=4&s=256";
  private static final String PUBLIC_IP = "185.199.108.133";
  private final AtomicInteger opened = new AtomicInteger();
  private final ByteArrayOutputStream sent = new ByteArrayOutputStream();

  private static InetAddress ip(String literal) {
    try {
      return InetAddress.getByName(literal);
    } catch (UnknownHostException e) {
      throw new IllegalStateException(e);
    }
  }

  private HttpAvatarImageFetcher fetcher(String address, String response) {
    return fetcherBytes(address, response.getBytes(StandardCharsets.ISO_8859_1));
  }

  private HttpAvatarImageFetcher fetcherBytes(String address, byte[] response) {
    return new HttpAvatarImageFetcher(
        host -> new InetAddress[] {ip(address)},
        (target, host, port) -> {
          opened.incrementAndGet();
          return new Socket() {
            @Override
            public InputStream getInputStream() {
              return new ByteArrayInputStream(response);
            }

            @Override
            public OutputStream getOutputStream() {
              return sent;
            }
          };
        });
  }

  private static String ok(String body) {
    return "HTTP/1.1 200 OK\r\nContent-Length: " + body.length() + "\r\n\r\n" + body;
  }

  @Test
  void aPublicAllowedHostIsDownloadedAndTheRequestKeepsTheSizeQuery() {
    var result = fetcher(PUBLIC_IP, ok("PNGDATA")).fetch(GOOD);

    assertThat(result).isPresent();
    assertThat(new String(result.get(), StandardCharsets.ISO_8859_1)).isEqualTo("PNGDATA");
    assertThat(sent.toString(StandardCharsets.US_ASCII))
        .startsWith("GET /u/1?v=4&s=256 HTTP/1.1")
        .contains("Host: avatars.githubusercontent.com");
  }

  @Test
  void aChunkedBodyIsReassembled() {
    String response =
        "HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n3\r\nabc\r\n2\r\nde\r\n0\r\n\r\n";
    var result = fetcher(PUBLIC_IP, response).fetch(GOOD);
    assertThat(new String(result.orElseThrow(), StandardCharsets.ISO_8859_1)).isEqualTo("abcde");
  }

  @Test
  void httpIsRefusedWithoutConnecting() {
    assertThat(fetcher(PUBLIC_IP, ok("x")).fetch(GOOD.replace("https", "http"))).isEmpty();
    assertThat(opened).hasValue(0);
  }

  @Test
  void aHostOutsideTheListIsRefusedWithoutConnecting() {
    var fetcher = fetcher(PUBLIC_IP, ok("x"));
    assertThat(fetcher.fetch("https://evil.example.com/a.png")).isEmpty();
    assertThat(fetcher.fetch("https://avatars.githubusercontent.com.evil.test/a.png")).isEmpty();
    assertThat(fetcher.fetch("https://user@avatars.githubusercontent.com/a.png")).isEmpty();
    assertThat(fetcher.fetch("https://avatars.githubusercontent.com:8443/a.png")).isEmpty();
    assertThat(opened).hasValue(0);
  }

  @Test
  void anAllowedHostResolvingToAnInternalAddressIsRefusedWithoutConnecting() {
    String[] internal = {
      "127.0.0.1",
      "10.1.2.3",
      "192.168.0.9",
      "172.16.5.5",
      "169.254.169.254",
      "0.0.0.0",
      "::1",
      "fd00::1",
      "fe80::1"
    };
    for (String address : internal) {
      assertThat(fetcher(address, ok("x")).fetch(GOOD)).as(address).isEmpty();
    }
    assertThat(opened).hasValue(0);
  }

  @Test
  void oneInternalAddressAmongPublicOnesRefusesTheName() {
    var fetcher =
        new HttpAvatarImageFetcher(
            host -> new InetAddress[] {ip(PUBLIC_IP), ip("127.0.0.1")},
            (a, h, p) -> {
              opened.incrementAndGet();
              return new Socket();
            });
    assertThat(fetcher.fetch(GOOD)).isEmpty();
    assertThat(opened).hasValue(0);
  }

  @Test
  void anUnresolvableNameIsRefused() {
    var fetcher =
        new HttpAvatarImageFetcher(
            host -> {
              throw new UnknownHostException(host);
            },
            (a, h, p) -> new Socket());
    assertThat(fetcher.fetch(GOOD)).isEmpty();
  }

  @Test
  void everyRedirectIsAFailureAndIsNotFollowed() {
    for (int status : new int[] {301, 302, 303, 307, 308}) {
      String response =
          "HTTP/1.1 " + status + " Moved\r\nLocation: https://169.254.169.254/\r\n\r\n";
      assertThat(fetcher(PUBLIC_IP, response).fetch(GOOD)).as("status " + status).isEmpty();
    }
    assertThat(opened).hasValue(5);
  }

  @Test
  void aDeclaredLengthOverTheCapIsRefused() {
    String response = "HTTP/1.1 200 OK\r\nContent-Length: 524289\r\n\r\n";
    assertThat(fetcher(PUBLIC_IP, response).fetch(GOOD)).isEmpty();
  }

  @Test
  void aBodyThatGrowsPast512KbWithoutALengthIsCutOff() {
    byte[] head = "HTTP/1.1 200 OK\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1);
    byte[] all = new byte[head.length + 524_289 + 10];
    System.arraycopy(head, 0, all, 0, head.length);
    assertThat(fetcherBytes(PUBLIC_IP, all).fetch(GOOD)).isEmpty();
  }

  @Test
  void aChunkedBodyOverTheCapIsCutOff() {
    String response = "HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n80001\r\n";
    assertThat(fetcher(PUBLIC_IP, response).fetch(GOOD)).isEmpty();
  }

  @Test
  void aTruncatedOrGarbageAnswerAndAnEmptyBodyAreRefused() {
    assertThat(fetcher(PUBLIC_IP, "HTTP/1.1 200").fetch(GOOD)).isEmpty();
    assertThat(fetcher(PUBLIC_IP, "garbage\r\n\r\n").fetch(GOOD)).isEmpty();
    assertThat(fetcher(PUBLIC_IP, ok("")).fetch(GOOD)).isEmpty();
  }

  @Test
  void aConnectionFailureIsRefusedNotThrown() {
    var fetcher =
        new HttpAvatarImageFetcher(
            host -> new InetAddress[] {ip(PUBLIC_IP)},
            (a, h, p) -> {
              throw new IOException("down");
            });
    assertThat(fetcher.fetch(GOOD)).isEmpty();
    assertThat(fetcher.fetch("not a url")).isEmpty();
  }
}
