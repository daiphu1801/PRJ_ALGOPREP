package com.algoprep.identity.infrastructure.storage;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.identity.application.ports.out.AvatarObjectStorePort;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import java.io.ByteArrayInputStream;
import java.util.Set;
import java.util.function.Supplier;

/**
 * MinIO adapter of the avatar bucket. Every SDK or I/O failure becomes {@code 503 IDT-511} without
 * the source message (it can carry endpoint and bucket); a missing object on read is {@code 404
 * IDT-451}, the same answer as no avatar at all.
 */
final class MinioAvatarObjectStore implements AvatarObjectStorePort {
  private static final Set<String> MISSING = Set.of("NoSuchKey", "NoSuchObject", "NotFound");

  private final Supplier<MinioClient> client;
  private final String bucket;

  MinioAvatarObjectStore(Supplier<MinioClient> client, String bucket) {
    this.client = client;
    this.bucket = bucket;
  }

  @Override
  public void put(String objectKey, byte[] data, String contentType) {
    try (var stream = new ByteArrayInputStream(data)) {
      client
          .get()
          .putObject(
              PutObjectArgs.builder().bucket(bucket).object(objectKey).stream(
                      stream, data.length, -1)
                  .contentType(contentType)
                  .build());
    } catch (Exception failure) {
      throw unavailable(failure);
    }
  }

  @Override
  public byte[] get(String objectKey) {
    try (var response =
        client.get().getObject(GetObjectArgs.builder().bucket(bucket).object(objectKey).build())) {
      return response.readAllBytes();
    } catch (ErrorResponseException failure) {
      if (failure.errorResponse() != null && MISSING.contains(failure.errorResponse().code())) {
        throw new ResourceNotFoundException(
            IdentityErrorCodes.AVATAR_NOT_FOUND, "Avatar object is missing");
      }
      throw unavailable(failure);
    } catch (Exception failure) {
      throw unavailable(failure);
    }
  }

  @Override
  public void delete(String objectKey) {
    try {
      client
          .get()
          .removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
    } catch (Exception failure) {
      throw unavailable(failure);
    }
  }

  private static ServiceUnavailableException unavailable(Exception failure) {
    return new ServiceUnavailableException(
        IdentityErrorCodes.AVATAR_STORAGE_UNAVAILABLE, "Avatar storage is unavailable", failure);
  }
}
