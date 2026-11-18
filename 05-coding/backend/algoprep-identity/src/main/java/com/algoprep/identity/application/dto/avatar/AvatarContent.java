package com.algoprep.identity.application.dto.avatar;

import java.util.function.Supplier;

/**
 * Result of reading an avatar. The bytes are read lazily by {@link #body()}, after the database
 * transaction has ended, so no connection is held while object storage answers.
 *
 * @param notModified true when {@code If-None-Match} matched; {@code body} is then null
 */
public record AvatarContent(
    String contentType, String etag, boolean notModified, Supplier<byte[]> body) {}
