package com.algoprep.problembank.infrastructure.storage;

import com.algoprep.problembank.application.ports.out.TestcaseObjectStorePort;
import com.algoprep.problembank.domain.exception.TestcaseStorageUnavailableException;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** Service reads never use public URLs. Signing may use a separate browser-reachable endpoint. */
public final class MinioTestcaseObjectStore implements TestcaseObjectStorePort {
  private final Supplier<MinioClient> service;
  private final Supplier<MinioClient> signer;
  private final String bucket;
  private final Duration attemptTimeout;

  public MinioTestcaseObjectStore(
      Supplier<MinioClient> service, Supplier<MinioClient> signer, String bucket) {
    this(service, signer, bucket, Duration.ofSeconds(10));
  }

  MinioTestcaseObjectStore(
      Supplier<MinioClient> service,
      Supplier<MinioClient> signer,
      String bucket,
      Duration attemptTimeout) {
    this.service = service;
    this.signer = signer;
    this.bucket = bucket;
    this.attemptTimeout = attemptTimeout;
  }

  @Override
  public String presignPut(String key, Duration ttl) {
    return presign(key, ttl, Method.PUT, 900);
  }

  @Override
  public String presignGet(String key, Duration ttl) {
    return presign(key, ttl, Method.GET, 300);
  }

  private String presign(String key, Duration ttl, Method method, int maximumSeconds) {
    validateKey(key);
    if (ttl == null
        || ttl.getNano() != 0
        || ttl.getSeconds() < 1
        || ttl.getSeconds() > maximumSeconds) {
      throw new IllegalArgumentException("Invalid testcase URL lifetime");
    }
    return execute(
        false,
        () ->
            signer
                .get()
                .getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                        .method(method)
                        .bucket(bucket)
                        .object(key)
                        .expiry((int) ttl.getSeconds())
                        .build()));
  }

  @Override
  public long head(String key) {
    validateKey(key);
    return execute(
        true,
        () ->
            service
                .get()
                .statObject(StatObjectArgs.builder().bucket(bucket).object(key).build())
                .size());
  }

  @Override
  public byte[] get(String key) {
    validateKey(key);
    return execute(
        true,
        () -> {
          try (var response =
              service.get().getObject(GetObjectArgs.builder().bucket(bucket).object(key).build())) {
            return response.readAllBytes();
          }
        });
  }

  @Override
  public void put(String key, byte[] bytes) {
    validateKey(key);
    if (bytes == null) {
      throw new IllegalArgumentException("Missing testcase bytes");
    }
    execute(
        false,
        () -> {
          try (var stream = new ByteArrayInputStream(bytes)) {
            service
                .get()
                .putObject(
                    PutObjectArgs.builder().bucket(bucket).object(key).stream(
                            stream, bytes.length, -1)
                        .contentType("application/octet-stream")
                        .build());
          }
          return null;
        });
  }

  @Override
  public void copy(String source, String destination) {
    validateKey(source);
    validateKey(destination);
    execute(
        false,
        () -> {
          service
              .get()
              .copyObject(
                  CopyObjectArgs.builder()
                      .bucket(bucket)
                      .object(destination)
                      .source(CopySource.builder().bucket(bucket).object(source).build())
                      .build());
          return null;
        });
  }

  @Override
  public void delete(String key) {
    validateKey(key);
    execute(
        false,
        () -> {
          service.get().removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
          return null;
        });
  }

  private static void validateKey(String key) {
    if (key == null
        || !key.startsWith("problems/")
        || key.endsWith("/")
        || key.contains("\\")
        || key.chars().anyMatch(Character::isISOControl)) {
      throw new IllegalArgumentException("Invalid testcase object key");
    }
    for (String segment : key.split("/")) {
      if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
        throw new IllegalArgumentException("Invalid testcase object key");
      }
    }
  }

  private <T> T execute(boolean retryRead, StorageAction<T> operation) {
    int attempts = retryRead ? 2 : 1;
    boolean objectMissing = false;
    for (int attempt = 0; attempt < attempts; attempt++) {
      try {
        return attempt(operation);
      } catch (Exception failure) {
        Throwable cause = failure instanceof ExecutionException ? failure.getCause() : failure;
        objectMissing = retryRead && isMissingObject(cause);
        if (failure instanceof InterruptedException
            || (failure instanceof ExecutionException
                && failure.getCause() instanceof InterruptedException)) {
          Thread.currentThread().interrupt();
        }
        if (Thread.currentThread().isInterrupted()) {
          break;
        }
      }
    }
    throw new TestcaseStorageUnavailableException(objectMissing);
  }

  private static boolean isMissingObject(Throwable failure) {
    return failure instanceof ErrorResponseException response
        && response.errorResponse() != null
        && response.errorResponse().code() != null
        && java.util.Set.of("NoSuchKey", "NoSuchObject", "NotFound")
            .contains(response.errorResponse().code());
  }

  /** Bounds the whole SDK operation, including multipart transfers and consuming GET streams. */
  private <T> T attempt(StorageAction<T> operation) throws Exception {
    var task =
        new FutureTask<T>(
            () -> {
              try {
                return operation.run();
              } catch (Exception problem) {
                if (Thread.currentThread().isInterrupted()) {
                  throw new InterruptedException();
                }
                throw problem;
              }
            });
    Thread.ofVirtual().start(task);
    try {
      return task.get(attemptTimeout.toNanos(), TimeUnit.NANOSECONDS);
    } finally {
      task.cancel(true);
    }
  }

  @FunctionalInterface
  private interface StorageAction<T> {
    T run() throws Exception;
  }
}
