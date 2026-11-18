package com.algoprep.identity.domain.model.avatar;

/**
 * What the header of an accepted avatar file says (DD validation F1 section 15).
 *
 * @param contentType {@code image/png}, {@code image/jpeg} or {@code image/webp}
 * @param extension {@code png}, {@code jpg} or {@code webp}: the object key suffix
 */
public record AvatarImage(String contentType, String extension, int width, int height) {}
