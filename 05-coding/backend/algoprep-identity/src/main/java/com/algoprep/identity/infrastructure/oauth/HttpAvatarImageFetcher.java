package com.algoprep.identity.infrastructure.oauth;

import com.algoprep.identity.application.ports.out.AvatarImageFetchPort;
import com.algoprep.identity.domain.model.avatar.AvatarImportRules;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Downloads a provider avatar under every rule of BD security F1 section 6.5. The name is resolved
 * once here, every resolved address must be public, and the socket is opened to that very address
 * (TLS name and SNI stay the host), so a second DNS answer cannot redirect the request. The
 * HTTP/1.1 exchange is written by hand because {@code HttpClient} resolves names itself. Any 3xx is
 * a failure, never followed. Failures log the host and a reason, never the URL.
 */
@Component
class HttpAvatarImageFetcher implements AvatarImageFetchPort {
  private static final Logger LOG = LoggerFactory.getLogger(HttpAvatarImageFetcher.class);
  private static final int HTTPS_PORT = 443;
  private static final int MAX_LINE = 8192;
  private static final int MAX_HEADERS = 64;
  private static final long TOTAL_NANOS = 10_000_000_000L;

  /** Name to addresses; replaced in tests so no DNS is needed. */
  interface Resolver {
    InetAddress[] resolve(String host) throws UnknownHostException;
  }

  /** Opens the connection to an already checked address; replaced in tests so no TLS is needed. */
  interface Connector {
    Socket open(InetAddress address, String host, int port) throws IOException;
  }

  private final Resolver resolver;
  private final Connector connector;

  @Autowired
  HttpAvatarImageFetcher() {
    this(InetAddress::getAllByName, HttpAvatarImageFetcher::tlsSocket);
  }

  HttpAvatarImageFetcher(Resolver resolver, Connector connector) {
    this.resolver = resolver;
    this.connector = connector;
  }

  @Override
  public Optional<byte[]> fetch(String url) {
    URI uri;
    try {
      uri = URI.create(url);
    } catch (RuntimeException malformed) {
      return refuse("unknown", "malformed-url");
    }
    String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
    String logHost = host.isEmpty() ? "unknown" : host;
    if (!"https".equals(uri.getScheme())) {
      return refuse(logHost, "scheme-not-https");
    }
    int port = uri.getPort() == -1 ? HTTPS_PORT : uri.getPort();
    if (!AvatarImportRules.ALLOWED_HOSTS.contains(host)
        || uri.getUserInfo() != null
        || port != HTTPS_PORT) {
      return refuse(logHost, "host-not-allowed");
    }
    InetAddress target = checkedAddress(host);
    if (target == null) {
      return refuse(host, "address-not-public");
    }
    String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
    String request = uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
    try (Socket socket = connector.open(target, host, port)) {
      socket.setSoTimeout(AvatarImportRules.READ_TIMEOUT_MILLIS);
      return exchange(socket, host, request);
    } catch (IOException failure) {
      return refuse(host, "io-" + failure.getClass().getSimpleName());
    }
  }

  /** The first address when every address of the name is public, else null. */
  private InetAddress checkedAddress(String host) {
    try {
      InetAddress[] all = resolver.resolve(host);
      if (all == null || all.length == 0) {
        return null;
      }
      for (InetAddress address : all) {
        if (!isPublic(address)) {
          return null;
        }
      }
      return all[0];
    } catch (UnknownHostException unknown) {
      return null;
    }
  }

  static boolean isPublic(InetAddress a) {
    byte[] raw = a.getAddress();
    boolean uniqueLocalV6 = raw.length == 16 && (raw[0] & 0xFE) == 0xFC;
    return !(a.isLoopbackAddress()
        || a.isLinkLocalAddress()
        || a.isSiteLocalAddress()
        || a.isAnyLocalAddress()
        || a.isMulticastAddress()
        || uniqueLocalV6);
  }

  private Optional<byte[]> exchange(Socket socket, String host, String request) throws IOException {
    long deadline = System.nanoTime() + TOTAL_NANOS;
    OutputStream out = socket.getOutputStream();
    String head =
        "GET "
            + request
            + " HTTP/1.1\r\nHost: "
            + host
            + "\r\nUser-Agent: AlgoPrep\r\nAccept: image/*\r\n"
            + "Accept-Encoding: identity\r\nConnection: close\r\n\r\n";
    out.write(head.getBytes(StandardCharsets.US_ASCII));
    out.flush();
    InputStream in = socket.getInputStream();
    String[] parts = readLine(in).split(" ", 3);
    if (parts.length < 2 || !parts[0].startsWith("HTTP/1.")) {
      return refuse(host, "bad-status-line");
    }
    if (!"200".equals(parts[1])) {
      return refuse(host, "status-" + parts[1]);
    }
    Headers headers = readHeaders(in);
    if (headers.length() > AvatarImportRules.MAX_BYTES) {
      return refuse(host, "too-large");
    }
    byte[] body =
        headers.chunked() ? readChunked(in, deadline) : readPlain(in, headers.length(), deadline);
    if (body == null) {
      return refuse(host, "too-large-or-slow");
    }
    return body.length == 0 ? refuse(host, "empty-body") : Optional.of(body);
  }

  private record Headers(long length, boolean chunked) {}

  /** Reads up to the blank line; a malformed or oversized header block is an IOException. */
  private static Headers readHeaders(InputStream in) throws IOException {
    long length = -1;
    boolean chunked = false;
    for (int i = 0; ; i++) {
      String line = readLine(in);
      if (line.isEmpty()) {
        return new Headers(length, chunked);
      }
      if (i >= MAX_HEADERS) {
        throw new IOException("too many headers");
      }
      int colon = line.indexOf(':');
      if (colon < 0) {
        continue;
      }
      String name = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
      String value = line.substring(colon + 1).trim();
      if ("content-length".equals(name)) {
        try {
          length = Long.parseLong(value);
        } catch (NumberFormatException bad) {
          throw new IOException("bad content-length");
        }
      } else if ("transfer-encoding".equals(name)
          && value.toLowerCase(Locale.ROOT).contains("chunked")) {
        chunked = true;
      }
    }
  }

  private static byte[] readPlain(InputStream in, long length, long deadline) throws IOException {
    var out = new ByteArrayOutputStream();
    byte[] buffer = new byte[8192];
    while (length < 0 || out.size() < length) {
      int want = length < 0 ? buffer.length : (int) Math.min(buffer.length, length - out.size());
      int n = in.read(buffer, 0, want);
      if (n < 0) {
        break;
      }
      out.write(buffer, 0, n);
      if (out.size() > AvatarImportRules.MAX_BYTES || System.nanoTime() > deadline) {
        return null;
      }
    }
    return out.toByteArray();
  }

  private static byte[] readChunked(InputStream in, long deadline) throws IOException {
    var out = new ByteArrayOutputStream();
    while (true) {
      String sizeLine = readLine(in);
      int semicolon = sizeLine.indexOf(';');
      String hex = (semicolon < 0 ? sizeLine : sizeLine.substring(0, semicolon)).trim();
      int size;
      try {
        size = Integer.parseInt(hex, 16);
      } catch (NumberFormatException bad) {
        throw new IOException("bad chunk size");
      }
      if (size == 0) {
        return out.toByteArray();
      }
      if (size < 0
          || out.size() + (long) size > AvatarImportRules.MAX_BYTES
          || System.nanoTime() > deadline) {
        return null;
      }
      out.write(in.readNBytes(size));
      readLine(in);
    }
  }

  private static String readLine(InputStream in) throws IOException {
    var line = new StringBuilder();
    while (true) {
      int b = in.read();
      if (b < 0) {
        throw new IOException("unexpected end of stream");
      }
      if (b == '\n') {
        int last = line.length() - 1;
        if (last >= 0 && line.charAt(last) == '\r') {
          line.setLength(last);
        }
        return line.toString();
      }
      line.append((char) b);
      if (line.length() > MAX_LINE) {
        throw new IOException("line too long");
      }
    }
  }

  private static Optional<byte[]> refuse(String host, String reason) {
    LOG.warn("Avatar download refused or failed: {} (host {})", reason, host);
    return Optional.empty();
  }

  private static Socket tlsSocket(InetAddress address, String host, int port) throws IOException {
    SSLSocket tls = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket();
    try {
      SSLParameters parameters = tls.getSSLParameters();
      parameters.setEndpointIdentificationAlgorithm("HTTPS");
      parameters.setServerNames(List.of(new SNIHostName(host)));
      tls.setSSLParameters(parameters);
      // The checked address, labelled with the host name so certificate checks use the host.
      InetAddress named = InetAddress.getByAddress(host, address.getAddress());
      tls.connect(new InetSocketAddress(named, port), AvatarImportRules.CONNECT_TIMEOUT_MILLIS);
      tls.setSoTimeout(AvatarImportRules.READ_TIMEOUT_MILLIS);
      tls.startHandshake();
      return tls;
    } catch (IOException | RuntimeException failure) {
      tls.close();
      throw failure;
    }
  }
}
