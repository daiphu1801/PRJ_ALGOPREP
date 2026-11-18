package com.algoprep.identity.application.ports.out;

/**
 * Object storage of avatar bytes (BD storage F1). Every method fails with {@code
 * ServiceUnavailableException} (IDT-511) when the store cannot answer, and {@link #get} with {@code
 * ResourceNotFoundException} (IDT-451) when the object is missing.
 */
public interface AvatarObjectStorePort {

  void put(String objectKey, byte[] data, String contentType);

  byte[] get(String objectKey);

  void delete(String objectKey);
}
